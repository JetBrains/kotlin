/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.builtins.functions.AllowedToUsedOnlyInK1
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.resolve.calls.inference.ForkPointData
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode.PARTIAL
import org.jetbrains.kotlin.resolve.calls.inference.components.InferenceLogger.FixationLogRecord
import org.jetbrains.kotlin.resolve.calls.inference.components.InferenceLogger.FixationLogVariableInfo
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableFixationFinder.Context
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableFixationFinder.VariableForFixation
import org.jetbrains.kotlin.resolve.calls.inference.hasRecursiveTypeParametersWithGivenSelfType
import org.jetbrains.kotlin.resolve.calls.inference.isRecursiveTypeParameter
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker
import org.jetbrains.kotlin.types.model.*
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * For the K1's DI to properly instantiate it with [LegacyVariableReadinessCalculator], this class must be `abstract`.
 */
@DefaultImplementation(VariableFixationFinder.DefaultForK1DependencyInjection::class)
abstract class VariableFixationFinder(
    private val languageVersionSettings: LanguageVersionSettings,
    private val variableReadinessCalculator: AbstractVariableReadinessCalculator<*>,
) {
    /**
     * Only used by the dependency injection in K1.
     */
    @OptIn(AllowedToUsedOnlyInK1::class)
    class DefaultForK1DependencyInjection(
        languageVersionSettings: LanguageVersionSettings,
        legacyVariableReadinessCalculator: LegacyVariableReadinessCalculator,
    ) : VariableFixationFinder(
        languageVersionSettings,
        legacyVariableReadinessCalculator,
    )

    class Default(
        languageVersionSettings: LanguageVersionSettings,
        variableReadinessCalculator: AbstractVariableReadinessCalculator<*>,
    ) : VariableFixationFinder(
        languageVersionSettings,
        variableReadinessCalculator,
    )

    interface Context : TypeSystemInferenceExtensionContext, ConstraintSystemMarker {
        val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
        val fixedTypeVariables: Map<TypeConstructorMarker, KotlinTypeMarker>
        val postponedTypeVariables: List<TypeVariableMarker>
        val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>>
        val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>

        /**
         * See [org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage.outerSystemVariablesPrefixSize]
         */
        val outerSystemVariablesPrefixSize: Int

        val outerTypeVariables: Set<TypeConstructorMarker>?
            get() =
                when {
                    outerSystemVariablesPrefixSize > 0 -> allTypeVariables.keys.take(outerSystemVariablesPrefixSize).toSet()
                    else -> null
                }

        /**
         * If not null, that property means that we should assume temporary them all as proper types when fixating some variables.
         *
         * By default, if that property is null, we assume all `allTypeVariables` as not proper.
         *
         * Currently, that is only used for `provideDelegate` resolution, see
         * [org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer.fixInnerVariablesForProvideDelegateIfNeeded]
         */
        val typeVariablesThatAreCountedAsProperTypes: Set<TypeConstructorMarker>?

        fun isReified(variable: TypeVariableMarker): Boolean
    }

    class VariableForFixation(
        val variable: TypeConstructorMarker,
        private val hasProperConstraint: Boolean,
        private val hasDependencyOnOuterTypeVariable: Boolean = false,
    ) {
        val isReady: Boolean get() = hasProperConstraint && !hasDependencyOnOuterTypeVariable
    }

    context(c: Context)
    fun findFirstVariableForFixation(
        allTypeVariables: List<TypeConstructorMarker>,
        postponedKtPrimitives: List<PostponedResolvedAtomMarker>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: KotlinTypeMarker,
    ): VariableForFixation? =
        findTypeVariableForFixation(allTypeVariables, postponedKtPrimitives, completionMode, topLevelType)

    context(c: Context)
    fun typeVariableHasProperConstraint(typeVariable: TypeConstructorMarker): Boolean {
        val dependencyProvider = TypeVariableDependencyInformationProvider(
            c.notFixedTypeVariables, emptyList(), topLevelType = null, c,
            languageVersionSettings,
        )

        return variableReadinessCalculator.typeVariableHasProperConstraint(typeVariable, dependencyProvider)
    }

    context(c: Context)
    private fun findTypeVariableForFixation(
        allTypeVariables: List<TypeConstructorMarker>,
        postponedArguments: List<PostponedResolvedAtomMarker>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: KotlinTypeMarker,
    ): VariableForFixation? {
        if (allTypeVariables.isEmpty()) return null

        val dependencyProvider = TypeVariableDependencyInformationProvider(
            c.notFixedTypeVariables, postponedArguments, topLevelType.takeIf { completionMode == PARTIAL }, c,
            languageVersionSettings,
        )

        val candidate = variableReadinessCalculator.chooseBestTypeVariableCandidateWithLogging(allTypeVariables, dependencyProvider)
            ?: return null
        return variableReadinessCalculator.prepareVariableForFixation(candidate, dependencyProvider)
    }
}

abstract class AbstractVariableReadinessCalculator<Readiness : Comparable<Readiness>>(
    private val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    private val languageVersionSettings: LanguageVersionSettings,
    inferenceLoggerParameter: InferenceLogger? = null,
) {
    /**
     * A workaround for K1's DI: the dummy instance must be provided, but
     * because it's useless, it's better to avoid calling its members to
     * prevent performance penalties.
     */
    @OptIn(AllowedToUsedOnlyInK1::class)
    private val inferenceLogger = inferenceLoggerParameter.takeIf { it !is InferenceLogger.Dummy }

    context(c: Context)
    abstract fun TypeConstructorMarker.getReadiness(dependencyProvider: TypeVariableDependencyInformationProvider): Readiness

    context(c: Context)
    abstract fun prepareVariableForFixation(
        candidate: TypeConstructorMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider
    ): VariableForFixation?

    context(c: Context)
    abstract fun typeVariableHasProperConstraint(
        typeVariable: TypeConstructorMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider,
    ): Boolean

    protected val fixationEnhancementsIn22: Boolean
        get() = languageVersionSettings.supportsFeature(LanguageFeature.FixationEnhancementsIn22)

    context(c: Context)
    protected fun TypeConstructorMarker.hasDirectConstraintToNotFixedRelevantVariable(): Boolean {
        return c.notFixedTypeVariables[this]?.constraints?.any { it.type.isNotFixedRelevantVariable() } == true
    }

    context(c: Context)
    protected fun TypeConstructorMarker.hasUnprocessedConstraintsInForks(): Boolean {
        if (c.constraintsFromAllForkPoints.isEmpty()) return false

        for ((_, forkPointData) in c.constraintsFromAllForkPoints) {
            for (constraints in forkPointData) {
                for ((typeVariableFromConstraint, constraint) in constraints) {
                    if (typeVariableFromConstraint.freshTypeConstructor() == this) return true
                    if (constraint.type.containsTypeVariable(this)) return true
                }
            }
        }

        return false
    }

    context(c: Context)
    protected fun TypeConstructorMarker.allConstraintsTrivialOrNonProper(): Boolean {
        return c.notFixedTypeVariables[this]?.constraints?.all { constraint ->
            trivialConstraintTypeInferenceOracle.isNotInterestingConstraint(constraint) || !constraint.isProperArgumentConstraint()
        } ?: false
    }

    context(c: Context)
    protected fun TypeConstructorMarker.hasOnlyIncorporatedConstraintsFromDeclaredUpperBound(): Boolean {
        val constraints = c.notFixedTypeVariables[this]?.constraints ?: return false

        fun Constraint.isTrivial() = kind == ConstraintKind.LOWER && type.isNothing()
                || kind == ConstraintKind.UPPER && type.isNullableAny()

        return constraints.filter { it.isProperArgumentConstraint() && !it.isTrivial() }.all { it.position.isFromDeclaredUpperBound }
    }

    @OptIn(K2Only::class)
    context(c: Context)
    fun chooseBestTypeVariableCandidateWithLogging(
        allTypeVariables: List<TypeConstructorMarker>,
        dependencyProvider: TypeVariableDependencyInformationProvider,
    ): TypeConstructorMarker? {
        if (inferenceLogger == null) {
            return allTypeVariables.maxByOrNull { it.getReadiness(dependencyProvider) }
        }

        val readinessPerVariable = allTypeVariables.associateWith {
            FixationLogVariableInfo(
                it.getReadiness(dependencyProvider),
                c.notFixedTypeVariables[it]?.constraints.orEmpty()
            )
        }
        val chosen = readinessPerVariable.entries.maxByOrNull { (_, value) -> value.readiness }?.key
        val newRecord = FixationLogRecord(
            readinessPerVariable.mapKeys { (key, _) -> c.allTypeVariables[key]!! }, c.allTypeVariables[chosen]
        )

        inferenceLogger.logReadiness(newRecord, c)
        return chosen
    }

    context(c: Context)
    protected fun TypeConstructorMarker.hasDependencyToOtherTypeVariables(): Boolean {
        val constraints = c.notFixedTypeVariables[this]?.constraints ?: return false
        return constraints.any { it.hasDependencyToOtherTypeVariable(this) }
    }

    context(c: Context)
    private fun Constraint.hasDependencyToOtherTypeVariable(ownerTypeVariable: TypeConstructorMarker): Boolean {
        return type.lowerBoundIfFlexible().argumentsCount() != 0 &&
                type.contains { it.typeConstructor() != ownerTypeVariable && c.notFixedTypeVariables.containsKey(it.typeConstructor()) }
    }

    // IltRelatedFlags can't be a combination of 1/0, as any non-ILT equality proper constraint is also a non-ILT proper constraint
    protected data class IltRelatedFlags(
        /**
         * @return true if a considered type variable has a proper EQUALS constraint T = SomeType, and SomeType is not an ILT-type
         */
        val hasProperNonIltEqualityConstraint: Boolean,
        /**
         * @return true if a considered type variable has a proper constraint T vs SomeType, and SomeType is not an ILT-type
         */
        val hasProperNonIltConstraint: Boolean,
    )

    context(c: Context)
    protected fun TypeConstructorMarker.computeIltConstraintsRelatedFlags(): IltRelatedFlags {
        val constraints = c.notFixedTypeVariables[this]?.constraints
        if (!fixationEnhancementsIn22 || constraints == null) return IltRelatedFlags(false, false)

        var hasProperNonIltEqualityConstraint = false
        var hasProperNonIltConstraint = false

        for (it in constraints) {
            val isProper = it.isProperArgumentConstraint()
            val containsIlt = it.type.contains { it.typeConstructor().isIntegerLiteralTypeConstructor() }
            val isProperNonIlt = isProper && !containsIlt

            hasProperNonIltEqualityConstraint = hasProperNonIltEqualityConstraint || isProperNonIlt && it.kind == ConstraintKind.EQUALITY
            hasProperNonIltConstraint = hasProperNonIltConstraint || isProperNonIlt
        }

        return IltRelatedFlags(hasProperNonIltEqualityConstraint, hasProperNonIltConstraint)
    }

    context(c: Context)
    protected fun TypeConstructorMarker.hasProperArgumentConstraints(): Boolean {
        val constraints = c.notFixedTypeVariables[this]?.constraints ?: return false
        val anyProperConstraint = constraints.any { it.isProperArgumentConstraint() }
        if (!anyProperConstraint) return false

        // temporary hack to fail calls which contain callable references resolved though OI with uninferred type parameters
        val areThereConstraintsWithUninferredTypeParameter = constraints.any { c -> c.type.contains { it.isUninferredParameter() } }
        if (areThereConstraintsWithUninferredTypeParameter) return false

        // The code below is only relevant to [FirInferenceSession.semiFixTypeVariablesAllowingFixationToOtherOnes] case,
        // which is expected to be used only for semi-fixation of input types for input types for OverloadResolutionByLambdaReturnType.
        if (!c.allowSemiFixationToOtherTypeVariables) return true

        val properConstraints = constraints.filter { it.isProperArgumentConstraint() }
        if (properConstraints.any { it.kind != ConstraintKind.LOWER }) return true

        // NB: All proper constraints are LOWER here.
        // As a resulting type for such a type variable is the common supertype of all lower constraints, which is undefined
        // for a case when all the constraints are type variables _and_ there are more than one of them.
        // For details, see [NewCommonSuperTypeCalculator.commonSuperTypeForNotNullTypes]
        val commonSupertypeIsUndefined = properConstraints.size > 1 && properConstraints.all {
            it.type.typeConstructor() in c.notFixedTypeVariables
        }

        return !commonSupertypeIsUndefined
    }

    context(c: Context)
    protected fun Constraint.isProperArgumentConstraint() =
        type.isProperType()
                && position.initialConstraint.position !is DeclaredUpperBoundConstraintPosition<*>
                && !isNullabilityConstraint
                && !isNoInfer

    context(c: Context)
    private fun KotlinTypeMarker.isProperType(): Boolean =
        isProperTypeForFixation(
            c.notFixedTypeVariables.keys
        ) { t -> !t.contains { it.isNotFixedRelevantVariable() } }

    context(c: Context)
    private fun KotlinTypeMarker.isNotFixedRelevantVariable(): Boolean {
        val key = typeConstructor()
        if (!c.notFixedTypeVariables.containsKey(key)) return false
        if (c.typeVariablesThatAreCountedAsProperTypes?.contains(key) == true) return false
        return true
    }

    context(c: Context)
    protected fun TypeConstructorMarker.isReified(): Boolean =
        c.notFixedTypeVariables[this]?.typeVariable?.let { c.isReified(it) } ?: false

    context(c: Context)
    private fun Constraint.isProperSelfTypeConstraint(ownerTypeVariable: TypeConstructorMarker): Boolean {
        val typeConstructor = type.typeConstructor()
        return position.from is DeclaredUpperBoundConstraintPosition<*>
                && (typeConstructor.hasRecursiveTypeParametersWithGivenSelfType() || typeConstructor.isRecursiveTypeParameter())
                && !hasDependencyToOtherTypeVariable(ownerTypeVariable)
    }

    context(c: Context)
    protected fun TypeConstructorMarker.areAllProperConstraintsSelfTypeBased(): Boolean {
        val constraints = c.notFixedTypeVariables[this]?.constraints?.takeIf { it.isNotEmpty() } ?: return false

        var hasSelfTypeConstraint = false
        var hasOtherProperConstraint = false

        for (constraint in constraints) {
            if (constraint.isProperSelfTypeConstraint(this)) {
                hasSelfTypeConstraint = true
            }
            if (constraint.isProperArgumentConstraint()) {
                hasOtherProperConstraint = true
            }
            if (hasSelfTypeConstraint && hasOtherProperConstraint) break
        }

        return hasSelfTypeConstraint && !hasOtherProperConstraint
    }
}

/**
 * Returns `false` for fixed type variables types even if `isProper(type) == true`
 * Thus allowing only non-TVs types to be used for fixation on top level.
 * While this limitation is important, it doesn't really limit final results because when we have a constraint like T <: E or E <: T
 * and we're going to fix T into E, we assume that if E has some other constraints, they are being incorporated to T, so we would choose
 * them instead of E itself.
 */
context(c: TypeSystemInferenceExtensionContext)
inline fun KotlinTypeMarker.isProperTypeForFixation(
    notFixedTypeVariables: Set<TypeConstructorMarker>,
    isProper: (KotlinTypeMarker) -> Boolean
): Boolean {
    // We don't allow fixing T into any top-level TV type, like T := F or T := F & Any
    // Even if F is considered as a proper by `isProper` (e.g., it belongs to an outer CS)
    // But at the same time, we don't forbid fixing into T := MutableList<F>
    // Exception: semi-fixing to other type variables is allowed during overload resolution by lambda return type
    if (!c.allowSemiFixationToOtherTypeVariables && typeConstructor() in notFixedTypeVariables) {
        return false
    }
    return isProper(this) && extractProjectionsForAllCapturedTypes().all(isProper)
}

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.extractProjectionsForAllCapturedTypes(): Set<KotlinTypeMarker> {
    if (isFlexible()) {
        val flexibleType = asFlexibleType()!!
        return buildSet {
            addAll(flexibleType.lowerBound().extractProjectionsForAllCapturedTypes())
            addAll(flexibleType.upperBound().extractProjectionsForAllCapturedTypes())
        }
    }
    val simpleBaseType = asRigidType()?.asCapturedTypeUnwrappingDnn()

    return buildSet {
        val projectionType = if (simpleBaseType != null) {
            val argumentType = simpleBaseType.typeConstructorProjection().getType() ?: return@buildSet
            argumentType.also(::add)
        } else {
            this@extractProjectionsForAllCapturedTypes
        }
        val argumentsCount = projectionType.argumentsCount().takeIf { it != 0 } ?: return@buildSet

        for (i in 0 until argumentsCount) {
            val argumentType = projectionType.getArgument(i).getType() ?: continue
            addAll(argumentType.extractProjectionsForAllCapturedTypes())
        }
    }
}

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.containsTypeVariable(typeVariable: TypeConstructorMarker): Boolean {
    if (contains { it.typeConstructor().unwrapStubTypeVariableConstructor() == typeVariable }) return true

    val typeProjections = extractProjectionsForAllCapturedTypes()

    return typeProjections.any { typeProjectionsType ->
        typeProjectionsType.contains { it.typeConstructor().unwrapStubTypeVariableConstructor() == typeVariable }
    }
}
