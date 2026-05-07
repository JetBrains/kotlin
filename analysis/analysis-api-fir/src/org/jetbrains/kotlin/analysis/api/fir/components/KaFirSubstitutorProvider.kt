/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaSubstitutorProvider
import org.jetbrains.kotlin.analysis.api.components.KaUnificationSubstitutorPolicy
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirTypeParameterSymbolBase
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirGenericSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirMapBackedSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.fir.resolve.calls.overloads.ConeSimpleConstraintSystemImpl
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeParameterBasedTypeVariable
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.jetbrains.kotlin.fir.resolve.substitution.*
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.substitutorForSuperType
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDirectionCalculator
import org.jetbrains.kotlin.resolve.calls.inference.model.InferredEmptyIntersection
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class KaFirSubstitutorProvider(
    override val analysisSessionProvider: () -> KaFirSession
) : KaBaseSessionComponent<KaFirSession>(), KaSubstitutorProvider, KaFirSessionComponent {
    override fun createInheritanceTypeSubstitutor(subClass: KaClassSymbol, superClass: KaClassSymbol): KaSubstitutor? {
        withValidityAssertion {
            if (subClass == superClass) return KaSubstitutor.Empty(token)

            val baseFirSymbol = subClass.firSymbol
            val superFirSymbol = superClass.firSymbol
            val inheritancePath = collectInheritancePath(baseFirSymbol, superFirSymbol) ?: return null
            val substitutors = inheritancePath.map { [type, symbol] ->
                type.substitutorForSuperType(rootModuleSession, symbol)
            }
            return when (substitutors.size) {
                0 -> KaSubstitutor.Empty(token)
                else -> {
                    val chained = substitutors.reduce { left, right -> left.chain(right) }
                    firSymbolBuilder.typeBuilder.buildSubstitutor(chained)
                }
            }
        }
    }

    private fun collectInheritancePath(
        baseSymbol: FirClassSymbol<*>,
        superSymbol: FirClassSymbol<*>,
    ): List<Pair<ConeClassLikeType, FirRegularClassSymbol>>? {
        val stack = mutableListOf<Pair<ConeClassLikeType, FirRegularClassSymbol>>()
        var result: List<Pair<ConeClassLikeType, FirRegularClassSymbol>>? = null

        fun dfs(symbol: FirClassSymbol<*>) {
            for (superType in symbol.resolvedSuperTypes) {
                if (result != null) {
                    return
                }
                if (superType !is ConeClassLikeType) continue
                val superClassSymbol = superType.toRegularClassSymbol(rootModuleSession) ?: continue
                stack += superType to superClassSymbol
                if (superClassSymbol == superSymbol) {
                    result = stack.toList()
                    check(stack.removeLast().second == superClassSymbol)
                    break
                }
                dfs(superClassSymbol)
                check(stack.removeLast().second == superClassSymbol)
            }
        }

        dfs(baseSymbol)
        return result?.reversed()
    }

    override fun createSubstitutor(mappings: Map<KaTypeParameterSymbol, KaType>): KaSubstitutor = withValidityAssertion {
        if (mappings.isEmpty()) return KaSubstitutor.Empty(token)

        val substitution = buildMap {
            mappings.forEach { [typeParameterSymbol, type] ->
                check(typeParameterSymbol is KaFirTypeParameterSymbolBase<*>)
                check(type is KaFirType)
                put(typeParameterSymbol.firSymbol, type.coneType)
            }
        }

        return when (val coneSubstitutor = substitutorByMap(substitution, analysisSession.firSession)) {
            is ConeSubstitutorByMap -> KaFirMapBackedSubstitutor(coneSubstitutor, analysisSession.firSymbolBuilder)
            else -> KaFirGenericSubstitutor(coneSubstitutor, analysisSession.firSymbolBuilder)
        }
    }

    override fun createSubtypingUnificationSubstitutor(
        leftType: KaType,
        rightType: KaType,
        constructionPolicy: KaUnificationSubstitutorPolicy,
    ): KaSubstitutor? = withValidityAssertion {
        createSubtypingUnificationSubstitutor(listOf(leftType to rightType), constructionPolicy)
    }

    override fun createSubtypingUnificationSubstitutor(
        leftTypesToRightTypes: List<Pair<KaType, KaType>>,
        constructionPolicy: KaUnificationSubstitutorPolicy
    ): KaSubstitutor? = withValidityAssertion {
        with(analysisSession) {
            /**
             * Retrieves all top-level type arguments involved in the signature of [this].
             *
             * MyClass<A, Pair<A, B>, List<Int>>.unwrapTypeArguments() -> {A, B}
             */
            fun KaType.collectTypeArgumentsFromTheSignature(): Set<KaTypeParameterSymbol> {
                return when (this) {
                    is KaTypeParameterType -> setOf(symbol)
                    is KaClassType -> this.typeArguments.flatMapTo(mutableSetOf()) { typeProjection ->
                        typeProjection.type?.collectTypeArgumentsFromTheSignature() ?: emptySet()
                    }
                    is KaIntersectionType -> this.conjuncts.flatMapTo(mutableSetOf()) { it.collectTypeArgumentsFromTheSignature() }
                    is KaFlexibleType ->
                        (this.lowerBound.collectTypeArgumentsFromTheSignature() + this.upperBound.collectTypeArgumentsFromTheSignature()).toSet()
                    is KaCapturedType -> this.projection.type?.collectTypeArgumentsFromTheSignature() ?: emptySet()
                    is KaDefinitelyNotNullType -> this.original.collectTypeArgumentsFromTheSignature()
                    else -> emptySet()
                }
            }

            /**
             * Retrieves all type arguments [this] depends on. This includes direct type arguments as well as their transitive bounds.
             */
            fun KaType.getAllTypeArgumentDependencies(): Set<KaTypeParameterSymbol> {
                val processingList = collectTypeArgumentsFromTheSignature().toMutableList()
                val result = processingList.toMutableSet()
                while (processingList.isNotEmpty()) {
                    val typeArgument = processingList.pop()
                    val upperBounds = typeArgument.upperBounds.flatMap { it.collectTypeArgumentsFromTheSignature() }.ifEmpty { continue }
                    upperBounds.forEach { upperBound ->
                        if (result.add(upperBound)) {
                            processingList.add(upperBound)
                        }
                    }
                }
                return result
            }

            if (leftTypesToRightTypes.isEmpty()) {
                return KaSubstitutor.Empty(analysisSession.token)
            }

            val leftTypeParameters = mutableSetOf<KaTypeParameterSymbol>()
            val rightTypeParameters = mutableSetOf<KaTypeParameterSymbol>()
            leftTypesToRightTypes.forEach { [leftType, rightType] ->
                leftTypeParameters.addAll(leftType.getAllTypeArgumentDependencies())
                rightTypeParameters.addAll(rightType.getAllTypeArgumentDependencies())
            }

            /**
             * If types in all the pairs do not depend on any type parameters,
             * a regular [org.jetbrains.kotlin.analysis.api.components.KaTypeRelationChecker.isSubtypeOf] is called.
             */
            if (rightTypeParameters.isEmpty() && leftTypeParameters.isEmpty()) {
                return KaSubstitutor.Empty(analysisSession.token).takeIf {
                    leftTypesToRightTypes.all { [leftType, rightType] ->
                        leftType.isSubtypeOf(rightType)
                    }
                }
            }

            /**
             * Contains all free type parameters. Free type parameters are parameters for which the constraint system will try
             * to find a mapping to satisfy the constraints.
             */
            val freeTypeParameters = when (constructionPolicy) {
                KaUnificationSubstitutorPolicy.ASSIGN_LEFT -> leftTypeParameters - rightTypeParameters
                KaUnificationSubstitutorPolicy.ASSIGN_RIGHT -> rightTypeParameters - leftTypeParameters
                KaUnificationSubstitutorPolicy.ASSIGN_ALL -> (rightTypeParameters + leftTypeParameters).distinct()
            }

            val constraintSystem = ConeSimpleConstraintSystemImpl(firSession.inferenceComponents.createConstraintSystem(), firSession)
            val typeSubstitutor = constraintSystem.registerTypeVariablesAndGetSubstitutor(freeTypeParameters)

            val registeredConstraints =
                mutableListOf<Pair<ConeKotlinType, ConeKotlinType>>()

            with(constraintSystem.context) {
                leftTypesToRightTypes.forEach { [leftType, rightType] ->
                    val preparedLeftType = AbstractTypeChecker.prepareType(
                        constraintSystem.context,
                        typeSubstitutor.safeSubstitute(leftType.coneType)
                    ).let {
                        /**
                         * Here it's important to use [org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext.captureFromExpression] whenever possible.
                         * It sets the capture status to [org.jetbrains.kotlin.types.model.CaptureStatus.FROM_EXPRESSION] for captured types
                         * instead of [org.jetbrains.kotlin.types.model.CaptureStatus.FOR_SUBTYPING],
                         * which helps to avoid approximation of captured types during the constraint calculation.
                         *
                         * Otherwise, it can produce unwanted constraints like UPPER(Nothing) and LOWER(Any?) (from CapturedType(*) <:> TypeVariable(E))
                         * in the type inference context due to the approximation of captured types.
                         *
                         * @see org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintInjector.TypeCheckerStateForConstraintInjector.addNewIncorporatedConstraint
                         */
                        constraintSystem.context.captureFromExpression(it) ?: it
                    }

                    val substitutedRightType = typeSubstitutor.safeSubstitute(rightType.coneType)

                    registeredConstraints += preparedLeftType.asCone() to substitutedRightType.asCone()
                    constraintSystem.addSubtypeConstraint(preparedLeftType, substitutedRightType)
                }
            }

            if (constraintSystem.hasContradiction()) {
                return null
            }

            val fixedKaSubstitutorByMap = constraintSystem.fixTypeVariablesAndGetSubstitutor()

            return fixedKaSubstitutorByMap.takeIf {
                if (constraintSystem.hasContradiction()) {
                    // Some errors in the system can occur during the variable fixation, so they need to be additionally checked.
                    return@takeIf false
                }

                if (constraintSystem.system.errors.any { error -> error is InferredEmptyIntersection }) {
                    // `constraintSystem.hasContradiction` only checks ERROR-level errors in the system.
                    // InferredEmptyIntersection might be reported with WARNING-level depending on the language settings,
                    // so it should be checked explicitly
                    return@takeIf false
                }

                val currentRawSubstitutor = constraintSystem.system.buildCurrentSubstitutor().asCone()

                /**
                 * This sanity check ensures that substituted left types are subtypes of the substituted right types.
                 * It's run on the raw substitutor from the constraint system and with prepared types that were registered as constraints.
                 *
                 * Prepared types are needed for the proper handling of captured types and star projections.
                 * E.g., with
                 * leftType = A<*>
                 * rightType = A<T>
                 * the produced substitutor is
                 * T -> CapturedType(*).
                 * A<*> is not a subtype of A<CapturedType(*)> as `*` is unknown and can represent anything.
                 * However, the prepared left type is actually A<CapturedType(*)>,
                 * and this captured projection is actually where this `T -> CapturedType(*)` mapping comes from.
                 * So the type checker can easily verify that these two projections are the same.
                 * Just recapturing star projections is unsafe because recaptures of the same star projection are not considered equal.
                 *
                 * We operate with cone types and the cone substitutor here instead of using KaTypes and the KaSubstitutor because
                 * type parameter types are turned into [org.jetbrains.kotlin.fir.types.ConeTypeVariableType]s when prepared:
                 * T -> TypeVariable(T), A<T> -> A<TypeVariable(T)>
                 * ConeTypeVariableTypes are not supported by the Analysis API
                 * and are converted into [org.jetbrains.kotlin.analysis.api.types.KaErrorType]s in `asKaType`.
                 */
                currentRawSubstitutor.isUnificationCorrect(registeredConstraints)
            }
        }
    }

    /**
     * Creates a fresh constraint system and registers [freeTypeParameters].
     * Returns a substitutor that maps registered type parameter symbols to their default types.
     *
     * This is mostly a copy of [ConeSimpleConstraintSystemImpl.registerTypeVariables].
     * However, we need a custom implementation to be able to replace type parameters in their recursive bounds with star projections.
     */
    private fun ConeSimpleConstraintSystemImpl.registerTypeVariablesAndGetSubstitutor(freeTypeParameters: Collection<KaTypeParameterSymbol>): ConeSubstitutor {
        val coneTypeParameterList = freeTypeParameters.map { kaTypeParameter ->
            require(kaTypeParameter is KaFirTypeParameterSymbol)
            ConeTypeParameterLookupTag(kaTypeParameter.firSymbol)
        }

        val csBuilder = system.getBuilder()
        val substitutionMap = coneTypeParameterList.associateBy({ it.typeParameterSymbol }) {
            val variable = ConeTypeParameterBasedTypeVariable(it.typeParameterSymbol)
            csBuilder.registerVariable(variable)

            variable.defaultType
        }

        val typeSubstitutor = substitutorByMap(substitutionMap, analysisSession.firSession)
        for (typeParameter in coneTypeParameterList) {
            for (upperBound in typeParameter.symbol.resolvedBounds) {
                addSubtypeConstraint(
                    substitutionMap[typeParameter.typeParameterSymbol]
                        ?: errorWithAttachment("No ${typeParameter.symbol.fir::class.java} in substitution map") {
                            withFirEntry("typeParameter", typeParameter.symbol.fir)
                        },
                    typeSubstitutor.substituteOrSelf(
                        upperBound.coneType.replaceRecursiveTypeParameterWithStarProjection(typeParameter)
                    ),
                )
            }
        }

        return typeSubstitutor
    }

    /**
     * Fixes variables in [this] and provides a substitutor from the stored constraints.
     */
    private fun ConeSimpleConstraintSystemImpl.fixTypeVariablesAndGetSubstitutor(): KaSubstitutor {
        with(system) {
            val inferenceComponents = session.inferenceComponents

            while (notFixedTypeVariables.isNotEmpty()) {
                val variableForFixation =
                    inferenceComponents.variableFixationFinder.findFirstVariableForFixation(
                        allTypeVariables = notFixedTypeVariables.keys.toList(),
                        postponedKtPrimitives = emptyList(),
                        completionMode = ConstraintSystemCompletionMode.FULL,
                        // Can be any type. Only affects the fixation with completionMode = ConstraintSystemCompletionMode.PARTIAL
                        topLevelType = analysisSession.builtinTypes.nullableAny.coneType,
                    ) ?: break

                val variableWithConstraints = notFixedTypeVariables.getValue(variableForFixation.variable)
                val resultType = inferenceComponents.resultTypeResolver.findResultType(
                    variableWithConstraints,
                    TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN,
                )

                fixVariable(
                    variableWithConstraints.typeVariable,
                    resultType,
                    ConeFixVariableConstraintPosition(variableWithConstraints.typeVariable),
                )
            }


            /**
             * The substitutor acquired from the fixation is [org.jetbrains.kotlin.fir.resolve.substitution.ConeTypeSubstitutorByTypeConstructor].
             * For debug / rendering purposes and simplicity, it has to be turned into a map-based substitutor.
             */
            val substitution = fixedTypeVariables.mapNotNull { [constructor, type] ->
                val variable = allTypeVariables[constructor] as? ConeTypeParameterBasedTypeVariable ?: return@mapNotNull null
                variable.typeParameterSymbol to type.asCone()
            }.toMap()

            val coneSubstitutorByMap = substitutorByMap(
                substitution,
                analysisSession.firSession,
                allowIdenticalSubstitution = false
            )
            return coneSubstitutorByMap.toKaSubstitutor()
        }
    }

    private fun ConeSubstitutor.isUnificationCorrect(registeredConstraints: List<Pair<ConeKotlinType, ConeKotlinType>>): Boolean {
        return registeredConstraints.all { [subType, rightType] ->
            val substitutedSubType = substituteOrSelf(subType)
            val substitutedRightType = substituteOrSelf(rightType)
            substitutedSubType.isSubtypeOf(substitutedRightType, this@KaFirSubstitutorProvider.analysisSession.firSession)
        }
    }

    /**
     * Replaces all occurrences of [recursiveTypeParameter] in [this] with star projections.
     */
    private fun ConeKotlinType.replaceRecursiveTypeParameterWithStarProjection(
        recursiveTypeParameter: ConeTypeParameterLookupTag,
    ): ConeKotlinType = when (this) {
        is ConeIntersectionType -> mapTypes { it.replaceRecursiveTypeParameterWithStarProjection(recursiveTypeParameter) }
        else -> withArguments { projection ->
            val projectionType = projection.type ?: return@withArguments projection
            if (projectionType is ConeTypeParameterType && projectionType.lookupTag == recursiveTypeParameter) {
                ConeStarProjection
            } else {
                projection.replaceType(projectionType.replaceRecursiveTypeParameterWithStarProjection(recursiveTypeParameter))
            }
        }
    }
}
