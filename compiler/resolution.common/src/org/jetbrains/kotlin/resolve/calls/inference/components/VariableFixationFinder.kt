/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.builtins.functions.AllowedToUsedOnlyInK1
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.inference.ForkPointData
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode.PARTIAL
import org.jetbrains.kotlin.resolve.calls.inference.components.InferenceLogger.FixationLogRecord
import org.jetbrains.kotlin.resolve.calls.inference.components.InferenceLogger.FixationLogVariableInfo
import org.jetbrains.kotlin.resolve.calls.inference.hasRecursiveTypeParametersWithGivenSelfType
import org.jetbrains.kotlin.resolve.calls.inference.isRecursiveTypeParameter
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableFixationFinder.TypeVariableFixationReadinessQuality as Q

class VariableFixationFinder(
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


//        val hasContradiction: Boolean,
//        val dependencyToOtherTypeVariables: Boolean,
//        val dependencyToOuterTypeVariables: Boolean,
//        val isFromDeclaredUpperBound: Boolean,
//        val isFromArgumentConstraint: Boolean,
//        val isFromComposedDeclaredUpperBound: Boolean,
//        val isFromUpperBoundOfNestedTypeParameter: Boolean,
//        val isFromUpperBoundOfNestedOnlyInputTypesTypeParameter: Boolean,
//        val isFromArgumentConstraintWithNullableUpperBound: Boolean,

    enum class TypeVariableFixationReadinessQuality {
//        HAS_PROPER_UPPER_CONSTRAINT,
//        HAS_PROPER_EQUALITY_CONSTRAINT,
//        HAS_REIFIED_TYPE_PARAMETER,
//        HAS_CONSTRAINTS_WITH_LITERAL_TYPES,
//        HAS_SELF_TYPE_DEPENDENCY,

        HAS_PROPER_NON_ILT_CONSTRAINT,
        HAS_PROPER_NON_ILT_EQUALITY_CONSTRAINT,

        NOT_ONLY_HAS_INCORPORATED_CONSTRAINTS_FROM_DECLARED_UPPER_BOUND,
        HAS_NO_RELATION_TO_ANY_OUTPUT_TYPE,
        HAS_PROPER_NON_TRIVIAL_CONSTRAINTS,
        HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES,
        HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT,

        IS_READY_FOR_FIXATION_AND_HAS_PROPER_NON_NOTHING_LOWER_CONSTRAINT,

        IS_READY_FOR_FIXATION_REIFIED,
        IS_READY_FOR_FIXATION,
        IS_READY_FOR_FIXATION_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES, // Otherwise, either "declared..." or not all proper constraints are self-type-based
//        ALL_PROPER_CONSTRAINTS_ARE_SELF_TYPE_BASED,

        HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY,
        HAS_PROPER_CONSTRAINTS,
        ALLOWED,
        ;

        val mask = 1 shl ordinal
    }

    class TypeVariableFixationReadiness : Comparable<TypeVariableFixationReadiness> {
        private var bitMask: Int = 0

        operator fun get(index: TypeVariableFixationReadinessQuality) = bitMask and index.mask != 0

        operator fun set(index: TypeVariableFixationReadinessQuality, value: Boolean) {
            val numerical = if (value) 1 else 0
            bitMask = (bitMask or index.mask) - index.mask * (1 - numerical)
        }

        override fun compareTo(other: TypeVariableFixationReadiness): Int = bitMask - other.bitMask

        override fun toString(): String {
            // `asReversed()` - to keep high-priority qualities first.
            val qualities = TypeVariableFixationReadinessQuality.entries.asReversed()
                .joinToString("\n\t") { (if (get(it)) " true " else "false ") + it.name }
            return "Readiness(\n\t$qualities\n)"
        }
    }

//    enum class TypeVariableFixationReadiness2 {
//        FORBIDDEN,
//        WITHOUT_PROPER_ARGUMENT_CONSTRAINT, // proper constraint from arguments -- not from upper bound for type parameters
//        OUTER_TYPE_VARIABLE_DEPENDENCY, // PCLA-only readiness
//
//        // This is used for self-type-based bounds and deprioritized in 1.5+.
//        // 2.2+ uses this kind of readiness for reified type parameters only, otherwise
//        // READY_FOR_FIXATION_CAPTURED_UPPER is in use
//        READY_FOR_FIXATION_DECLARED_UPPER_BOUND_WITH_SELF_TYPES,
//
//        // After 2.2, `WITH_COMPLEX_DEPENDENCY` means the variable only contains non-proper constraints or constraints with ILT.
//        WITH_COMPLEX_DEPENDENCY, // if type variable T has constraint with non fixed type variable inside (non-top-level): T <: Foo<S>
//        WITH_COMPLEX_DEPENDENCY_AND_PROPER_NON_ILT, // Same as before but also has a constraint to types like `Long`, `Int`, etc.
//        WITH_COMPLEX_DEPENDENCY_AND_PROPER_NON_ILT_EQUALITY, // Same as WITH_COMPLEX_DEPENDENCY but also has a constraint T = ... not dependent on others
//        ALL_CONSTRAINTS_TRIVIAL_OR_NON_PROPER, // proper trivial constraint from arguments, Nothing <: T
//        RELATED_TO_ANY_OUTPUT_TYPE,
//        FROM_INCORPORATION_OF_DECLARED_UPPER_BOUND,
//
//        // We prefer LOWER T >: SomeRegularType to UPPER T <: SomeRegularType, KT-41934 is the only reason known
//        READY_FOR_FIXATION_UPPER,
//        READY_FOR_FIXATION_LOWER,
//
//        // Currently used in 2.2+ ONLY for self-type based declared upper bounds in particular situations
//        // Captured types are difficult to manipulate, so with T <: Captured(...) AND T <: K
//        // it's better to fix T earlier than K >: SomeRegularType / K <: SomeRegularType,
//        // as otherwise we will have T <: Captured(...) & SomeRegularType
//        // which is often problematic
//        // TODO: it would be probably better to use READY_FOR_FIXATION_UPPER here and to have
//        // it prioritized in comparison with READY_FOR_FIXATION_LOWER (however, KT-41934 example currently prevents it)
//        READY_FOR_FIXATION_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES,
//
//        // K1 used this for reified type parameters, mainly to get discriminateNothingForReifiedParameter.kt working
//        // KT-55691 lessens the need for this readiness kind in K2,
//        // however K2 still needs this e.g. for reifiedToNothing.kt example.
//        // TODO: consider deprioritizing Nothing in relation systems like Nothing <: T <: SomeType (see KT-76443)
//        // and not using anymore this readiness kind in K2. Related issues: KT-32358 (especially kt32358_3.kt test)
//        READY_FOR_FIXATION_REIFIED,
//    }

    private val fixationEnhancementsIn22: Boolean
        get() = languageVersionSettings.supportsFeature(LanguageFeature.FixationEnhancementsIn22)

    context(c: Context)
    private fun TypeConstructorMarker.getReadiness(
        dependencyProvider: TypeVariableDependencyInformationProvider
    ): TypeVariableFixationReadiness {
        val result = TypeVariableFixationReadiness().also {
            val forbidden = !c.notFixedTypeVariables.contains(this)
                    || dependencyProvider.isVariableRelatedToTopLevelType(this)
                    || hasUnprocessedConstraintsInForks()
            it[Q.ALLOWED] = !forbidden
            it[Q.HAS_PROPER_CONSTRAINTS] = hasProperArgumentConstraints() || areAllProperConstraintsSelfTypeBased()
            it[Q.HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY] = !dependencyProvider.isRelatedToOuterTypeVariable(this)

//            it[Q.ALL_PROPER_CONSTRAINTS_ARE_SELF_TYPE_BASED] = areAllProperConstraintsSelfTypeBased()
            it[Q.IS_READY_FOR_FIXATION_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES] = areAllProperConstraintsSelfTypeBased()
                    && fixationEnhancementsIn22
                    && !isReified() && hasDirectConstraintToNotFixedRelevantVariable()

//            if (areAllProperConstraintsSelfTypeBased() && !it[Q.IS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES]) {
//                return@also
//            }

            it[Q.HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT] = it[Q.HAS_PROPER_CONSTRAINTS] && !areAllProperConstraintsSelfTypeBased()
            it[Q.HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES] = !hasDependencyToOtherTypeVariables()

            if (it[Q.HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES]) {
                it[Q.HAS_PROPER_NON_TRIVIAL_CONSTRAINTS] = !allConstraintsTrivialOrNonProper()

                if (it[Q.HAS_PROPER_NON_TRIVIAL_CONSTRAINTS]) {
                    it[Q.HAS_NO_RELATION_TO_ANY_OUTPUT_TYPE] = !dependencyProvider.isVariableRelatedToAnyOutputType(this)
                    it[Q.NOT_ONLY_HAS_INCORPORATED_CONSTRAINTS_FROM_DECLARED_UPPER_BOUND] = !hasOnlyIncorporatedConstraintsFromDeclaredUpperBound()
                }
            }

            computeReadinessForVariableWithDependencies(it)

            it[Q.IS_READY_FOR_FIXATION] = it[Q.HAS_PROPER_CONSTRAINTS]
                    && it[Q.HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY]
                    && !hasDependencyToOtherTypeVariables()
                    && !allConstraintsTrivialOrNonProper()
                    && !dependencyProvider.isVariableRelatedToAnyOutputType(this)
                    && !hasOnlyIncorporatedConstraintsFromDeclaredUpperBound()

            if (it[Q.IS_READY_FOR_FIXATION]) {
                it[Q.IS_READY_FOR_FIXATION_REIFIED] = isReified()
                it[Q.IS_READY_FOR_FIXATION_AND_HAS_PROPER_NON_NOTHING_LOWER_CONSTRAINT] = hasLowerNonNothingProperConstraint()
            }
        }
        return result


//        return when {
//            !c.notFixedTypeVariables.contains(this) || dependencyProvider.isVariableRelatedToTopLevelType(this) ||
//                    hasUnprocessedConstraintsInForks() ->
//                TypeVariableFixationReadiness.FORBIDDEN
//
//            // Pre-2.2: might be fixed, but this condition should come earlier than the next one,
//            // because self-type-based cases do not have proper constraints, though they assumed to be fixed
//            // 2.2+: self-type-based upper bounds are considered captured upper bounds
//            // (update: only in particular situations with another constraint like T <: K available, see KT-80577),
//            // and have higher priority as upper/lower (affects e.g. KT-74999)
//            // For reified variables we keep old behavior, as captured types aren't usable for their substitutions (see KT-49838, KT-51040)
//            // See other comments for READY_FOR_FIXATION_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES itself
//            areAllProperConstraintsSelfTypeBased() -> {
//                if (fixationEnhancementsIn22 && !isReified() && hasDirectConstraintToNotFixedRelevantVariable()) {
//                    TypeVariableFixationReadiness.READY_FOR_FIXATION_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
//                } else {
//                    TypeVariableFixationReadiness.READY_FOR_FIXATION_DECLARED_UPPER_BOUND_WITH_SELF_TYPES
//                }
//            }
//
//            // Prevents from fixation
//            !hasProperArgumentConstraints() -> TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT
//            // PCLA only
//            dependencyProvider.isRelatedToOuterTypeVariable(this) -> TypeVariableFixationReadiness.OUTER_TYPE_VARIABLE_DEPENDENCY
//
//            // All cases below do not prevent fixation but just define the priority order of a variable
//            hasDependencyToOtherTypeVariables() -> computeReadinessForVariableWithDependencies()
//            // TODO: Consider removing this kind of readiness, see KT-63032
//            allConstraintsTrivialOrNonProper() -> TypeVariableFixationReadiness.ALL_CONSTRAINTS_TRIVIAL_OR_NON_PROPER
//            dependencyProvider.isVariableRelatedToAnyOutputType(this) -> TypeVariableFixationReadiness.RELATED_TO_ANY_OUTPUT_TYPE
//            hasOnlyIncorporatedConstraintsFromDeclaredUpperBound() ->
//                TypeVariableFixationReadiness.FROM_INCORPORATION_OF_DECLARED_UPPER_BOUND
//            isReified() -> TypeVariableFixationReadiness.READY_FOR_FIXATION_REIFIED
//
//            // 1.5+ (questionable) logic: we prefer LOWER constraints to UPPER constraints, mostly because of KT-41934
//            // TODO: try to reconsider (see KT-76518)
//            hasLowerNonNothingProperConstraint() -> TypeVariableFixationReadiness.READY_FOR_FIXATION_LOWER
//            else -> TypeVariableFixationReadiness.READY_FOR_FIXATION_UPPER
//        }
    }

    context(c: Context)
    private fun TypeConstructorMarker.hasDirectConstraintToNotFixedRelevantVariable(): Boolean {
        return c.notFixedTypeVariables[this]?.constraints?.any { it.type.isNotFixedRelevantVariable() } == true
    }

    context(c: Context)
    private fun TypeConstructorMarker.hasUnprocessedConstraintsInForks(): Boolean {
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
    fun typeVariableHasProperConstraint(typeVariable: TypeConstructorMarker): Boolean {
        val dependencyProvider = TypeVariableDependencyInformationProvider(
            c.notFixedTypeVariables, emptyList(), topLevelType = null, c,
            languageVersionSettings,
        )
        val readiness = typeVariable.getReadiness(dependencyProvider)
        return when {
            !readiness[Q.ALLOWED] || !readiness[Q.HAS_PROPER_CONSTRAINTS] -> false
            else -> true
        }
    }

    context(c: Context)
    private fun TypeConstructorMarker.allConstraintsTrivialOrNonProper(): Boolean {
        return c.notFixedTypeVariables[this]?.constraints?.all { constraint ->
            trivialConstraintTypeInferenceOracle.isNotInterestingConstraint(constraint) || !constraint.isProperArgumentConstraint()
        } ?: false
    }

    context(c: Context)
    private fun TypeConstructorMarker.hasOnlyIncorporatedConstraintsFromDeclaredUpperBound(): Boolean {
        val constraints = c.notFixedTypeVariables[this]?.constraints ?: return false

        return constraints.filter { it.isProperArgumentConstraint() }.all { it.position.isFromDeclaredUpperBound }
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

        val candidate = chooseBestTypeVariableCandidateWithLogging(allTypeVariables, dependencyProvider) ?: return null
        val readiness = candidate.getReadiness(dependencyProvider)

//        VariableForFixation(
//            candidate,
//            readiness[Q.HAS_PROPER_CONSTRAINTS],
//            readiness[Q.HAS_OUTER_TYPE_VARIABLE_DEPENDENCY]
//        )

        return when {
            !readiness[Q.ALLOWED] -> null
            !readiness[Q.HAS_PROPER_CONSTRAINTS] -> VariableForFixation(candidate, false)
            !readiness[Q.HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY] ->
                VariableForFixation(candidate, hasProperConstraint = true, hasDependencyOnOuterTypeVariable = true)

            else -> VariableForFixation(candidate, true)
        }
    }

    @OptIn(K2Only::class)
    context(c: Context)
    private fun chooseBestTypeVariableCandidateWithLogging(
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
    private fun TypeConstructorMarker.hasDependencyToOtherTypeVariables(): Boolean {
        val constraints = c.notFixedTypeVariables[this]?.constraints ?: return false
        return constraints.any { it.hasDependencyToOtherTypeVariable(this) }
    }

    context(c: Context)
    private fun Constraint.hasDependencyToOtherTypeVariable(ownerTypeVariable: TypeConstructorMarker): Boolean {
        return type.lowerBoundIfFlexible().argumentsCount() != 0 &&
                type.contains { it.typeConstructor() != ownerTypeVariable && c.notFixedTypeVariables.containsKey(it.typeConstructor()) }
    }

//    context(c: Context)
//    private fun TypeConstructorMarker.computeReadinessForVariableWithDependencies(): TypeVariableFixationReadiness {
//        val constraints = c.notFixedTypeVariables[this]?.constraints
//        if (!fixationEnhancementsIn22 || constraints == null) return TypeVariableFixationReadiness.WITH_COMPLEX_DEPENDENCY
//
//        var hasProperNonIltEqualityConstraint = false
//        var hasProperNonIltConstraint = false
//
//        for (it in constraints) {
//            val isProper = it.isProperArgumentConstraint()
//            val containsIlt = it.type.contains { it.typeConstructor().isIntegerLiteralTypeConstructor() }
//            val isProperNonIlt = isProper && !containsIlt
//
//            hasProperNonIltEqualityConstraint = hasProperNonIltEqualityConstraint || isProperNonIlt && it.kind == ConstraintKind.EQUALITY
//            hasProperNonIltConstraint = hasProperNonIltConstraint || isProperNonIlt
//        }
//
//        return when {
//            hasProperNonIltEqualityConstraint -> TypeVariableFixationReadiness.WITH_COMPLEX_DEPENDENCY_AND_PROPER_NON_ILT_EQUALITY
//            hasProperNonIltConstraint -> TypeVariableFixationReadiness.WITH_COMPLEX_DEPENDENCY_AND_PROPER_NON_ILT
//            else -> TypeVariableFixationReadiness.WITH_COMPLEX_DEPENDENCY
//        }
//    }

    context(c: Context)
    private fun TypeConstructorMarker.computeReadinessForVariableWithDependencies(readiness: TypeVariableFixationReadiness) {
        val constraints = c.notFixedTypeVariables[this]?.constraints
        if (!fixationEnhancementsIn22 || constraints == null) return

        var hasProperNonIltEqualityConstraint = false
        var hasProperNonIltConstraint = false

        for (it in constraints) {
            val isProper = it.isProperArgumentConstraint()
            val containsIlt = it.type.contains { it.typeConstructor().isIntegerLiteralTypeConstructor() }
            val isProperNonIlt = isProper && !containsIlt

            hasProperNonIltEqualityConstraint = hasProperNonIltEqualityConstraint || isProperNonIlt && it.kind == ConstraintKind.EQUALITY
            hasProperNonIltConstraint = hasProperNonIltConstraint || isProperNonIlt
        }

        readiness[Q.HAS_PROPER_NON_ILT_EQUALITY_CONSTRAINT] = hasProperNonIltEqualityConstraint
        readiness[Q.HAS_PROPER_NON_ILT_CONSTRAINT] = hasProperNonIltConstraint
    }

    context(c: Context)
    private fun TypeConstructorMarker.hasProperArgumentConstraints(): Boolean {
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
    private fun Constraint.isProperArgumentConstraint() =
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
    private fun TypeConstructorMarker.isReified(): Boolean =
        c.notFixedTypeVariables[this]?.typeVariable?.let { c.isReified(it) } ?: false

    context(c: Context)
    private fun TypeConstructorMarker.hasLowerNonNothingProperConstraint(): Boolean {
        val constraints = c.notFixedTypeVariables[this]?.constraints ?: return false

        return constraints.any {
            it.kind.isLower() && it.isProperArgumentConstraint() && !it.type.typeConstructor().isNothingConstructor()
        }
    }

    context(c: Context)
    private fun Constraint.isProperSelfTypeConstraint(ownerTypeVariable: TypeConstructorMarker): Boolean {
        val typeConstructor = type.typeConstructor()
        return position.from is DeclaredUpperBoundConstraintPosition<*>
                && (typeConstructor.hasRecursiveTypeParametersWithGivenSelfType() || typeConstructor.isRecursiveTypeParameter())
                && !hasDependencyToOtherTypeVariable(ownerTypeVariable)
    }

    context(c: Context)
    private fun TypeConstructorMarker.areAllProperConstraintsSelfTypeBased(): Boolean {
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
