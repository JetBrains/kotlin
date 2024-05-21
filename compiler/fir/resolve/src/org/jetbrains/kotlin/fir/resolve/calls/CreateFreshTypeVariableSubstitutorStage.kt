/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeParameterBasedTypeVariable
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeDeclaredUpperBoundConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition

internal object CreateFreshTypeVariableSubstitutorStage : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val declaration = candidate.symbol.fir
        candidate.symbol.lazyResolveToPhase(FirResolvePhase.STATUS)
        if (declaration !is FirTypeParameterRefsOwner || declaration.typeParameters.isEmpty()) {
            candidate.substitutor = ConeSubstitutor.Empty
            candidate.freshVariables = emptyList()
            return
        }
        val csBuilder = candidate.system.getBuilder()
        val (substitutor, freshVariables) =
            createToFreshVariableSubstitutorAndAddInitialConstraints(declaration, csBuilder, context.session)
        candidate.substitutor = substitutor
        candidate.freshVariables = freshVariables

        // bad function -- error on declaration side
        if (csBuilder.hasContradiction) {
            sink.yieldDiagnostic(InapplicableCandidate)
            return
        }

        // optimization
        if (candidate.typeArgumentMapping == TypeArgumentMapping.NoExplicitArguments /*&& knownTypeParametersResultingSubstitutor == null*/) {
            return
        }

        val typeParameters = declaration.typeParameters
        for (index in typeParameters.indices) {
            val typeParameter = typeParameters[index]
            val freshVariable = freshVariables[index]

            when (val typeArgument = candidate.typeArgumentMapping[index]) {
                is FirTypeProjectionWithVariance -> csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    getTypePreservingFlexibilityWrtTypeVariable(
                        typeArgument.typeRef.coneType,
                        typeParameter,
                        context.session
                    ).fullyExpandedType(context.session),
                    ConeExplicitTypeParameterConstraintPosition(typeArgument)
                )
                is FirStarProjection -> csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    typeParameter.symbol.resolvedBounds.firstOrNull()?.coneType
                        ?: context.session.builtinTypes.nullableAnyType.type,
                    SimpleConstraintSystemConstraintPosition
                )
                else -> assert(typeArgument is FirPlaceholderProjection) {
                    "Unexpected typeArgument: ${typeArgument.renderWithType()}"
                }
            }
        }
        if (csBuilder.hasContradiction) {
            for (error in csBuilder.errors) {
                sink.reportDiagnostic(InferenceError(error))
            }
            sink.yieldIfNeed()
        }
    }

    /**
     * TODO: Get rid of this function once KT-59138 is fixed and the relevant feature for disabling it will be removed
     * This function provides a type for a newly created EQUALS constraint on a fresh type variable,
     * for a situation when we have an explicit type argument and type parameter is a Java type parameter without known nullability.
     *
     * For a normal function call, like foo<T = SomeType>, we create a constraint T = SomeType!.
     * This is an unsafe solution, however yet we have to keep it, otherwise a lot of code becomes red.
     * Typical "strange" example:
     *
     * ```
     * // Java
     * public class Foo {
     *     static <T> T id(T foo) {
     *         return null;
     *     }
     * }
     *
     * // Kotlin
     * fun test(): String {
     *     return Foo.id<String?>(null) // OK...
     * }
     * ```
     *
     * We keep more sound constraint T = SomeType for regular and SAM constructor calls. Typical examples are:
     *
     * ```
     * fun test1() = J1<Int>() // type should be J1<Int>, not J1<Int!>
     * // J1.java
     * public class J1<T1> {}
     * ```
     *
     * or
     *
     * ```
     * // Again, type should be J<String> and not J<String!>
     * fun test1() = J<String> { x -> x }
     *
     *
     * // FILE: J.java
     * public interface J<T> {
     *     T foo(T x);
     * }
     * ```
     *
     * @return type which is chosen for EQUALS constraint
     */
    private fun getTypePreservingFlexibilityWrtTypeVariable(
        type: ConeKotlinType,
        typeParameter: FirTypeParameterRef,
        session: FirSession,
    ): ConeKotlinType {
        return if (typeParameter.shouldBeFlexible(session.typeContext)) {
            when (type) {
                is ConeSimpleKotlinType -> ConeFlexibleType(
                    type.withNullability(ConeNullability.NOT_NULL, session.typeContext),
                    type.withNullability(ConeNullability.NULLABLE, session.typeContext)
                )
                /*
                 * ConeFlexibleTypes have to be handled here
                 * at least because MapTypeArguments special-cases ConeRawTypes without explicit arguments (KT-54666)
                 * which allows them to get past the NoExplicitArguments optimization
                 * in CreateFreshTypeVariableSubstitutorStage.check
                 *
                 * (it might be safe to just return the same flexible type without explicitly enforcing flexibility,
                 * but better safe than sorry when dealing with raw types)
                 */
                is ConeFlexibleType -> ConeFlexibleType(
                    type.lowerBound.withNullability(ConeNullability.NOT_NULL, session.typeContext),
                    type.upperBound.withNullability(ConeNullability.NULLABLE, session.typeContext)
                )
            }
        } else {
            type
        }
    }

    private fun FirTypeParameterRef.shouldBeFlexible(context: ConeTypeContext): Boolean {
        if (context.session.languageVersionSettings.supportsFeature(LanguageFeature.JavaTypeParameterDefaultRepresentationWithDNN)) {
            return false
        }
        return symbol.resolvedBounds.any {
            val type = it.coneType
            type is ConeFlexibleType || with(context) {
                (type.typeConstructor() as? ConeTypeParameterLookupTag)?.symbol?.fir?.shouldBeFlexible(context) ?: false
            }
        }
    }
}

private fun createToFreshVariableSubstitutorAndAddInitialConstraints(
    declaration: FirTypeParameterRefsOwner,
    csBuilder: ConstraintSystemOperation,
    session: FirSession
): Pair<ConeSubstitutor, List<ConeTypeVariable>> {

    val typeParameters = declaration.typeParameters

    val freshTypeVariables = typeParameters.map { ConeTypeParameterBasedTypeVariable(it.symbol) }

    val toFreshVariables = substitutorByMap(freshTypeVariables.associate { it.typeParameterSymbol to it.defaultType }, session)

    for (freshVariable in freshTypeVariables) {
        csBuilder.registerVariable(freshVariable)
    }

    fun ConeTypeParameterBasedTypeVariable.addSubtypeConstraint(
        upperBound: ConeKotlinType//,
        //position: DeclaredUpperBoundConstraintPosition
    ) {
        if ((upperBound.lowerBoundIfFlexible() as? ConeClassLikeType)?.lookupTag?.classId == StandardClassIds.Any &&
            upperBound.upperBoundIfFlexible().isMarkedNullable
        ) {
            return
        }

        csBuilder.addSubtypeConstraint(
            defaultType,
            toFreshVariables.substituteOrSelf(upperBound),
            ConeDeclaredUpperBoundConstraintPosition()
        )
    }

    for (index in typeParameters.indices) {
        val typeParameter = typeParameters[index]
        val freshVariable = freshTypeVariables[index]

        val parameterSymbolFromExpandedClass = typeParameter.symbol.fir.getTypeParameterFromExpandedClass(index, session)

        for (upperBound in parameterSymbolFromExpandedClass.symbol.resolvedBounds) {
            freshVariable.addSubtypeConstraint(upperBound.coneType/*, position*/)
        }
    }

    return toFreshVariables to freshTypeVariables
}

private fun FirTypeParameter.getTypeParameterFromExpandedClass(index: Int, session: FirSession): FirTypeParameter {
    val containingDeclaration = containingDeclarationSymbol.fir
    if (containingDeclaration is FirRegularClass) {
        return containingDeclaration.typeParameters.elementAtOrNull(index)?.symbol?.fir ?: this
    } else if (containingDeclaration is FirTypeAlias) {
        val typeParameterConeType = toConeType()
        val expandedConeType = containingDeclaration.expandedTypeRef.coneType
        val typeArgumentIndex = expandedConeType.typeArguments.indexOfFirst { it.type == typeParameterConeType }
        val expandedTypeFir = expandedConeType.toSymbol(session)?.fir
        if (expandedTypeFir is FirTypeParameterRefsOwner) {
            val typeParameterFir = expandedTypeFir.typeParameters.elementAtOrNull(typeArgumentIndex)?.symbol?.fir ?: return this
            if (expandedTypeFir is FirTypeAlias) {
                return typeParameterFir.getTypeParameterFromExpandedClass(typeArgumentIndex, session)
            }
            return typeParameterFir
        }
    }

    return this
}
