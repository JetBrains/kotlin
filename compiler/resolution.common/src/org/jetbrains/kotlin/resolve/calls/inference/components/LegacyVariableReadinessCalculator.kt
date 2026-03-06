/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.isNothingConstructor
import org.jetbrains.kotlin.types.model.typeConstructor
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableFixationFinder.Context
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableFixationFinder.VariableForFixation

class LegacyVariableReadinessCalculator(
    trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    languageVersionSettings: LanguageVersionSettings,
    inferenceLoggerParameter: InferenceLogger? = null,
) : AbstractVariableReadinessCalculator<LegacyVariableReadinessCalculator.TypeVariableFixationReadiness>(
    trivialConstraintTypeInferenceOracle,
    languageVersionSettings,
    inferenceLoggerParameter,
) {
    enum class TypeVariableFixationReadiness {
        FORBIDDEN,
        WITHOUT_PROPER_ARGUMENT_CONSTRAINT, // proper constraint from arguments -- not from upper bound for type parameters
        OUTER_TYPE_VARIABLE_DEPENDENCY, // PCLA-only readiness

        // This is used for self-type-based bounds and deprioritized in 1.5+.
        // 2.2+ uses this kind of readiness for reified type parameters only, otherwise
        // READY_FOR_FIXATION_CAPTURED_UPPER is in use
        READY_FOR_FIXATION_DECLARED_UPPER_BOUND_WITH_SELF_TYPES,

        // After 2.2, `WITH_COMPLEX_DEPENDENCY` means the variable only contains non-proper constraints or constraints with ILT.
        WITH_COMPLEX_DEPENDENCY, // if type variable T has constraint with non fixed type variable inside (non-top-level): T <: Foo<S>
        WITH_COMPLEX_DEPENDENCY_AND_PROPER_NON_ILT, // Same as before but also has a constraint to types like `Long`, `Int`, etc.
        WITH_COMPLEX_DEPENDENCY_AND_PROPER_NON_ILT_EQUALITY, // Same as WITH_COMPLEX_DEPENDENCY but also has a constraint T = ... not dependent on others
        ALL_CONSTRAINTS_TRIVIAL_OR_NON_PROPER, // proper trivial constraint from arguments, Nothing <: T
        RELATED_TO_ANY_OUTPUT_TYPE,
        FROM_INCORPORATION_OF_DECLARED_UPPER_BOUND,

        // We prefer LOWER T >: SomeRegularType to UPPER T <: SomeRegularType, KT-41934 is the only reason known
        READY_FOR_FIXATION_UPPER,
        READY_FOR_FIXATION_LOWER,

        // Currently used in 2.2+ ONLY for self-type based declared upper bounds in particular situations
        // Captured types are difficult to manipulate, so with T <: Captured(...) AND T <: K
        // it's better to fix T earlier than K >: SomeRegularType / K <: SomeRegularType,
        // as otherwise we will have T <: Captured(...) & SomeRegularType
        // which is often problematic
        // TODO: it would be probably better to use READY_FOR_FIXATION_UPPER here and to have
        // it prioritized in comparison with READY_FOR_FIXATION_LOWER (however, KT-41934 example currently prevents it)
        READY_FOR_FIXATION_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES,

        // K1 used this for reified type parameters, mainly to get discriminateNothingForReifiedParameter.kt working
        // KT-55691 lessens the need for this readiness kind in K2,
        // however K2 still needs this e.g. for reifiedToNothing.kt example.
        // TODO: consider deprioritizing Nothing in relation systems like Nothing <: T <: SomeType (see KT-76443)
        // and not using anymore this readiness kind in K2. Related issues: KT-32358 (especially kt32358_3.kt test)
        READY_FOR_FIXATION_REIFIED
    }

    context(c: Context)
    override fun TypeConstructorMarker.getReadiness(
        dependencyProvider: TypeVariableDependencyInformationProvider
    ): TypeVariableFixationReadiness {
        return when {
            !c.notFixedTypeVariables.contains(this) || dependencyProvider.isVariableRelatedToTopLevelType(this) ||
                    hasUnprocessedConstraintsInForks() ->
                TypeVariableFixationReadiness.FORBIDDEN

            // Pre-2.2: might be fixed, but this condition should come earlier than the next one,
            // because self-type-based cases do not have proper constraints, though they assumed to be fixed
            // 2.2+: self-type-based upper bounds are considered captured upper bounds
            // (update: only in particular situations with another constraint like T <: K available, see KT-80577),
            // and have higher priority as upper/lower (affects e.g. KT-74999)
            // For reified variables we keep old behavior, as captured types aren't usable for their substitutions (see KT-49838, KT-51040)
            // See other comments for READY_FOR_FIXATION_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES itself
            areAllProperConstraintsSelfTypeBased() -> {
                if (fixationEnhancementsIn22 && !isReified() && hasDirectConstraintToNotFixedRelevantVariable()) {
                    TypeVariableFixationReadiness.READY_FOR_FIXATION_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
                } else {
                    TypeVariableFixationReadiness.READY_FOR_FIXATION_DECLARED_UPPER_BOUND_WITH_SELF_TYPES
                }
            }

            // Prevents from fixation
            !hasProperArgumentConstraints() -> TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT
            // PCLA only
            dependencyProvider.isRelatedToOuterTypeVariable(this) -> TypeVariableFixationReadiness.OUTER_TYPE_VARIABLE_DEPENDENCY

            // All cases below do not prevent fixation but just define the priority order of a variable
            hasDependencyToOtherTypeVariables() -> computeReadinessForVariableWithDependencies()
            // TODO: Consider removing this kind of readiness, see KT-63032
            allConstraintsTrivialOrNonProper() -> TypeVariableFixationReadiness.ALL_CONSTRAINTS_TRIVIAL_OR_NON_PROPER
            dependencyProvider.isVariableRelatedToAnyOutputType(this) -> TypeVariableFixationReadiness.RELATED_TO_ANY_OUTPUT_TYPE
            hasOnlyIncorporatedConstraintsFromDeclaredUpperBoundLegacyVersion() ->
                TypeVariableFixationReadiness.FROM_INCORPORATION_OF_DECLARED_UPPER_BOUND
            isReified() -> TypeVariableFixationReadiness.READY_FOR_FIXATION_REIFIED

            // 1.5+ (questionable) logic: we prefer LOWER constraints to UPPER constraints, mostly because of KT-41934
            // TODO: try to reconsider (see KT-76518)
            hasLowerNonNothingProperConstraint() -> TypeVariableFixationReadiness.READY_FOR_FIXATION_LOWER
            else -> TypeVariableFixationReadiness.READY_FOR_FIXATION_UPPER
        }
    }

    context(c: Context)
    private fun TypeConstructorMarker.hasLowerNonNothingProperConstraint(): Boolean {
        val constraints = c.notFixedTypeVariables[this]?.constraints ?: return false

        return constraints.any {
            it.kind.isLower() && it.isProperArgumentConstraint() && !it.type.typeConstructor().isNothingConstructor()
        }
    }

    context(c: Context)
    private fun TypeConstructorMarker.hasOnlyIncorporatedConstraintsFromDeclaredUpperBoundLegacyVersion(): Boolean {
        val constraints = c.notFixedTypeVariables[this]?.constraints ?: return false

        return constraints.filter { it.isProperArgumentConstraint() }.all { it.position.isFromDeclaredUpperBound }
    }

    context(c: Context)
    override fun typeVariableHasProperConstraint(
        typeVariable: TypeConstructorMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider,
    ): Boolean {
        return when (typeVariable.getReadiness(dependencyProvider)) {
            TypeVariableFixationReadiness.FORBIDDEN, TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT -> false
            else -> true
        }
    }

    context(c: Context)
    override fun prepareVariableForFixation(
        candidate: TypeConstructorMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider
    ): VariableForFixation? {
        return when (candidate.getReadiness(dependencyProvider)) {
            TypeVariableFixationReadiness.FORBIDDEN -> null
            TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT -> VariableForFixation(candidate, false)
            TypeVariableFixationReadiness.OUTER_TYPE_VARIABLE_DEPENDENCY ->
                VariableForFixation(candidate, hasProperConstraint = true, hasDependencyOnOuterTypeVariable = true)

            else -> VariableForFixation(candidate, true)
        }
    }

    context(c: Context)
    private fun TypeConstructorMarker.computeReadinessForVariableWithDependencies(): TypeVariableFixationReadiness {
        val (hasProperNonIltEqualityConstraint, hasProperNonIltConstraint) = computeIltConstraintsRelatedFlags()

        return when {
            hasProperNonIltEqualityConstraint -> TypeVariableFixationReadiness.WITH_COMPLEX_DEPENDENCY_AND_PROPER_NON_ILT_EQUALITY
            hasProperNonIltConstraint -> TypeVariableFixationReadiness.WITH_COMPLEX_DEPENDENCY_AND_PROPER_NON_ILT
            else -> TypeVariableFixationReadiness.WITH_COMPLEX_DEPENDENCY
        }
    }
}
