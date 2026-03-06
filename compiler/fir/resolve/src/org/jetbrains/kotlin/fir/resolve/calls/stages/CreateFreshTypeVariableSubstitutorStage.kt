/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.stages

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.OnlyForDefaultLanguageFeatureDisabled
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.calls.InapplicableCandidate
import org.jetbrains.kotlin.fir.resolve.calls.InferenceError
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CheckerSink
import org.jetbrains.kotlin.fir.resolve.calls.candidate.yieldDiagnostic
import org.jetbrains.kotlin.fir.resolve.calls.candidate.yieldIfNeed
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeParameterBasedTypeVariable
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeDeclaredUpperBoundConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.unwrapSubstitutionOverrides
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition

internal object CreateFreshTypeVariableSubstitutorStage : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        val declaration = candidate.symbol.fir
        candidate.symbol.lazyResolveToPhase(FirResolvePhase.STATUS)
        if (declaration !is FirTypeParameterRefsOwner || declaration.typeParameters.isEmpty()) {
            candidate.initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
            return
        }
        val csBuilder = candidate.system.getBuilder()
        val (substitutor, freshVariables) =
            createToFreshVariableSubstitutorAndAddInitialConstraints(declaration, csBuilder)
        candidate.initializeSubstitutorAndVariables(substitutor, freshVariables)

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
                    ).fullyExpandedType(),
                    ConeExplicitTypeParameterConstraintPosition(typeArgument)
                )
                is FirStarProjection -> csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    typeParameter.symbol.resolvedBounds.firstOrNull()?.coneType
                        ?: context.session.builtinTypes.nullableAnyType.coneType,
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
     * TODO: Get rid of this function once [LanguageFeature.DontMakeExplicitJavaTypeArgumentsFlexible] is removed
     *
     * @return type which is chosen for EQUALS constraint
     */
    context(context: ResolutionContext)
    private fun getTypePreservingFlexibilityWrtTypeVariable(
        type: ConeKotlinType,
        typeParameter: FirTypeParameterRef,
    ): ConeKotlinType {
        val session = context.session
        return if (typeParameter.shouldBeFlexible()) {
            when (type) {
                is ConeRigidType -> type.withNullability(nullable = false, session.typeContext).toTrivialFlexibleType(session.typeContext)
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
                    type.lowerBound.withNullability(nullable = false, session.typeContext),
                    type.upperBound.withNullability(nullable = true, session.typeContext),
                    isTrivial = false,
                )
            }.run {
                if (LanguageFeature.DontMakeExplicitJavaTypeArgumentsFlexible.isEnabled()) {
                    return@run this
                }
                if (!type.isMarkedNullable) {
                    return@run this
                }
                withAttributes(
                    attributes.add(
                        ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(
                            type, LanguageFeature.DontMakeExplicitJavaTypeArgumentsFlexible
                        )
                    )
                )
            }
        } else {
            type
        }
    }

    context(context: ResolutionContext)
    private fun FirTypeParameterRef.shouldBeFlexible(): Boolean {
        val languageVersionSettings = context.session.languageVersionSettings
        if (languageVersionSettings.supportsFeature(LanguageFeature.DontMakeExplicitJavaTypeArgumentsFlexible)) {
            return false
        }
        return symbol.resolvedBounds.any {
            val type = it.coneType
            type is ConeFlexibleType || with(context.typeContext) {
                (type.typeConstructor() as? ConeTypeParameterLookupTag)?.symbol?.fir?.shouldBeFlexible() ?: false
            }
        }
    }

    context(context: ResolutionContext)
    private fun createToFreshVariableSubstitutorAndAddInitialConstraints(
        declaration: FirTypeParameterRefsOwner,
        csBuilder: ConstraintSystemOperation,
    ): Pair<ConeSubstitutor, List<ConeTypeVariable>> {
        val typeParameters = declaration.typeParameters
        val freshTypeVariables = typeParameters.map { ConeTypeParameterBasedTypeVariable(it.symbol) }

        val toFreshVariables = substitutorByMap(freshTypeVariables.associate { it.typeParameterSymbol to it.defaultType }, context.session)
            .let {
                val typeAliasConstructorSubstitutor = (declaration as? FirConstructor)?.typeAliasConstructorInfo?.substitutor
                if (typeAliasConstructorSubstitutor != null) {
                    ChainedSubstitutor(typeAliasConstructorSubstitutor, it)
                } else {
                    it
                }
            }

        for (freshVariable in freshTypeVariables) {
            csBuilder.registerVariable(freshVariable)
        }

        val constraints = if (LanguageFeature.ProperlyCheckUpperBoundsViolationsWhenCreatingFreshVariables.isEnabled()) {
            addConstraintsProperly(declaration, toFreshVariables, freshTypeVariables)
        } else {
            @OptIn(OnlyForDefaultLanguageFeatureDisabled::class)
            addConstraintsTheOldWay(toFreshVariables, freshTypeVariables, typeParameters)
        }

        for ((lower, upper) in constraints) {
            csBuilder.addSubtypeConstraint(lower, upper, ConeDeclaredUpperBoundConstraintPosition())
        }

        return toFreshVariables to freshTypeVariables
    }

    context(context: ResolutionContext)
    private fun addConstraintsProperly(
        declaration: FirTypeParameterRefsOwner,
        toFreshVariables: ConeSubstitutor,
        freshTypeVariables: List<ConeTypeParameterBasedTypeVariable>,
    ): List<Pair<ConeKotlinType, ConeKotlinType>> {
        val typeAliasConstructorInfo = (declaration as? FirConstructor)?.typeAliasConstructorInfo
        val isTypealiasConstructor = typeAliasConstructorInfo != null

        val (typeArgumentsForConstraining, typeParametersForConstraining) = when {
            isTypealiasConstructor -> {
                val fullyExpandedType = declaration.unwrapSubstitutionOverrides().returnTypeRef.coneType.fullyExpandedType()
                val arguments = fullyExpandedType.let(toFreshVariables::substituteOrSelf).typeArguments.toList()
                val parameters = fullyExpandedType.toClassLikeSymbol()?.fir?.typeParameters ?: emptyList()
                arguments to parameters
            }
            else -> {
                freshTypeVariables.map { it.defaultType.toTypeProjection(ProjectionKind.INVARIANT) } to declaration.typeParameters
            }
        }

        val constraints = mutableListOf<Pair<ConeKotlinType, ConeKotlinType>>()

        for ((index, parameter) in typeParametersForConstraining.withIndex()) {
            val argumentType = typeArgumentsForConstraining.getOrNull(index)?.type?.let(toFreshVariables::substituteOrSelf) ?: continue

            for (bound in parameter.symbol.resolvedBounds) {
                val substitutedBound = toFreshVariables.substituteOrSelf(bound.coneType)
                val isRedundant = substitutedBound.lowerBoundIfFlexible().classLikeLookupTagIfAny?.classId == StandardClassIds.Any
                        && substitutedBound.upperBoundIfFlexible().isMarkedNullable

                if (!isRedundant) {
                    constraints += argumentType to substitutedBound
                }
            }
        }

        return constraints
    }

    @OnlyForDefaultLanguageFeatureDisabled(LanguageFeature.ProperlyCheckUpperBoundsViolationsWhenCreatingFreshVariables)
    context(context: ResolutionContext)
    private fun addConstraintsTheOldWay(
        toFreshVariables: ConeSubstitutor,
        freshTypeVariables: List<ConeTypeParameterBasedTypeVariable>,
        typeParameters: List<FirTypeParameterRef>,
    ): MutableList<Pair<ConeKotlinType, ConeKotlinType>> {
        val constraints = mutableListOf<Pair<ConeKotlinType, ConeKotlinType>>()

        fun ConeTypeParameterBasedTypeVariable.addSubtypeConstraint(
            upperBound: ConeKotlinType//,
            //position: DeclaredUpperBoundConstraintPosition
        ) {
            if (upperBound.lowerBoundIfFlexible().classLikeLookupTagIfAny?.classId == StandardClassIds.Any &&
                upperBound.upperBoundIfFlexible().isMarkedNullable
            ) {
                return
            }

            constraints += defaultType to toFreshVariables.substituteOrSelf(upperBound)
        }

        for (index in typeParameters.indices) {
            val typeParameter = typeParameters[index]
            val freshVariable = freshTypeVariables[index]

            val parameterSymbolFromExpandedClass = typeParameter.symbol.fir.getTypeParameterFromExpandedClass(index)

            for (upperBound in parameterSymbolFromExpandedClass.symbol.resolvedBounds) {
                freshVariable.addSubtypeConstraint(upperBound.coneType/*, position*/)
            }
        }

        return constraints
    }

    context(context: ResolutionContext)
    private fun FirTypeParameter.getTypeParameterFromExpandedClass(index: Int): FirTypeParameter {
        val containingDeclaration = containingDeclarationSymbol.fir
        if (containingDeclaration is FirRegularClass) {
            return containingDeclaration.typeParameters.elementAtOrNull(index)?.symbol?.fir ?: this
        } else if (containingDeclaration is FirTypeAlias) {
            val typeParameterConeType = toConeType()
            val expandedConeType = containingDeclaration.expandedTypeRef.coneType
            val typeArgumentIndex = expandedConeType.typeArguments.indexOfFirst { it.type == typeParameterConeType }
            val expandedTypeFir = expandedConeType.toSymbol()?.fir
            if (expandedTypeFir is FirTypeParameterRefsOwner) {
                val typeParameterFir = expandedTypeFir.typeParameters.elementAtOrNull(typeArgumentIndex)?.symbol?.fir ?: return this
                if (expandedTypeFir is FirTypeAlias) {
                    return typeParameterFir.getTypeParameterFromExpandedClass(typeArgumentIndex)
                }
                return typeParameterFir
            }
        }

        return this
    }
}