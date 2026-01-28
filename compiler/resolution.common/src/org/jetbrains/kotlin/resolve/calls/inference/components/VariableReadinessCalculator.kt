/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableFixationFinder.Context
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableFixationFinder.VariableForFixation
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableReadinessCalculator.TypeVariableFixationReadinessQuality as Q

class VariableReadinessCalculator(
    trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    languageVersionSettings: LanguageVersionSettings,
    inferenceLoggerParameter: InferenceLogger? = null,
) : AbstractVariableReadinessCalculator<VariableReadinessCalculator.TypeVariableFixationReadiness>(
    trivialConstraintTypeInferenceOracle,
    languageVersionSettings,
    inferenceLoggerParameter,
) {
    /**
     * Earlier values have higher priority.
     */
    enum class TypeVariableFixationReadinessQuality {
        // *** Strong de-prioritizers (normally, these are `true`, and they de-prioritize by being `false`) ***
        ALLOWED,

        // A proper constraint is a constraint that does not depend on other not-yet-fixed type variables, like T <: S or T <: Type<S>.
        // In PCLA case, a not-yet-fixed type variable in the argument position still gives us a proper constraint.
        HAS_PROPER_CONSTRAINTS,
        HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY,

        // *** The following block constitutes what "ready for fixation" used to mean in the old fixation code ***
        // Theoretically, materialize-like functions should be capable of returning
        // an instance of any arbitrary type, including `Nothing`, which is not possible,
        // but in practice, there are reasonable materialize-like implementations that we
        // want to support, and we need to make sure our type inference doesn't infer their
        // return types into something too specific that the actual implementation won't
        // be able to provide (see KT-74999 - the Traversable vs. Path choice).
        // The "self-sufficiency" condition prevents attempts to fix materialize variables
        // "too early" (see `k2StubTypeLeak.kt`).
        IS_SELF_SUFFICIENT_MATERIALIZE_VARIABLE,

        // Prioritizes the flow of type information between parallel branches in cases like
        // the `arg ?: MyClass()` choice (`inferredToProjectionInsteadOfParameter.kt`).
        HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT,
        HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES,

        // A proper trivial constraint from arguments, except for `T >: Nothing(?)`
        // It does however include `T = Nothing(?)` and `T <: Nothing(?)`
        HAS_PROPER_NON_TRIVIAL_CONSTRAINTS,
        HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND,

        // *** "ready for fixation" kinds ***
        // Prefer `LOWER` `T :> SomeRegularType` to `UPPER` `T <: SomeRegularType` for KT-41934.
        // Prefer `LOWER` constraint also to `EQUALS` `T = SomeRegularType` because of the test
        // FirLightTreeDiagnosticsWithLatestLanguageVersionTestGenerated.testJavaFunctionParamNullability.
        // TODO: KT-82574 (consider preferring EQUALS constraints)
        HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT,

        // *** The following block constitutes what "with complex dependency" used to mean in the old fixation code ***
        // Prioritizers needed for KT-67335 (the `greater.kt` case with ILTs).
        // ILT type = Integer literal type = yet unknown choice from Byte/Short/Int/Long (at least two of them)
        // Any proper constraint can be here which isn't bound to ILT type
        HAS_PROPER_NON_ILT_CONSTRAINT,
        ;

        init {
            check(ordinal < (Int.SIZE_BITS - 2))
        }

        val mask: Int = 1 shl ((Int.SIZE_BITS - 2) - ordinal)
    }

    class TypeVariableFixationReadiness : Comparable<TypeVariableFixationReadiness> {
        private var bitMask: Int = 0

        operator fun get(index: TypeVariableFixationReadinessQuality): Boolean = (bitMask and index.mask) != 0

        operator fun set(index: TypeVariableFixationReadinessQuality, value: Boolean) {
            val conditionalMask = if (value) index.mask else 0
            bitMask = (bitMask and index.mask.inv()) or conditionalMask
        }

        override fun compareTo(other: TypeVariableFixationReadiness): Int = bitMask - other.bitMask
    }

    context(c: Context)
    override fun TypeConstructorMarker.getReadiness(dependencyProvider: TypeVariableDependencyInformationProvider): TypeVariableFixationReadiness {
        val readiness = TypeVariableFixationReadiness()

        val forbidden = !c.notFixedTypeVariables.contains(this)
                || dependencyProvider.isVariableRelatedToTopLevelType(this)
                || hasUnprocessedConstraintsInForks()
        val areAllProperConstraintsSelfTypeBased = areAllProperConstraintsSelfTypeBased()

        // These values go in the same order as they are defined in `TypeVariableFixationReadinessQuality`,
        // except for being reversed: so that higher-priority ones come first.

        readiness[Q.ALLOWED] = !forbidden
        if (forbidden) return readiness

        readiness[Q.HAS_PROPER_CONSTRAINTS] = hasProperArgumentConstraints() || areAllProperConstraintsSelfTypeBased
        readiness[Q.HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY] = !dependencyProvider.isRelatedToOuterTypeVariable(this)

        val hasDependencyToOtherTypeVariables = hasDependencyToOtherTypeVariables()
        val isMaterializeVariable = this in c.returnTypeTypeVariables
                && true == c.notFixedTypeVariables[this]?.constraints?.none { it.kind.isLower() }
                && true == c.notFixedTypeVariables[this]?.constraints?.any {
            it.kind.isUpper() && (it.isProperArgumentConstraint() || it.isProperSelfTypeConstraint(this))
        }

        readiness[Q.IS_SELF_SUFFICIENT_MATERIALIZE_VARIABLE] = isMaterializeVariable
                && !hasDependencyToOtherTypeVariables

        readiness[Q.HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT] =
            readiness[Q.HAS_PROPER_CONSTRAINTS] && !areAllProperConstraintsSelfTypeBased
        readiness[Q.HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES] = !hasDependencyToOtherTypeVariables
        readiness[Q.HAS_PROPER_NON_TRIVIAL_CONSTRAINTS] = !allConstraintsTrivialOrNonProper()
        readiness[Q.HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND] =
            !hasOnlyIncorporatedConstraintsFromDeclaredUpperBound()

        readiness[Q.HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT] = hasLowerNonNothingNonIltProperConstraint()

        val (_, hasProperNonIltConstraint) = computeIltConstraintsRelatedFlags()
        readiness[Q.HAS_PROPER_NON_ILT_CONSTRAINT] = hasProperNonIltConstraint

        return readiness
    }

    context(c: Context)
    private fun TypeConstructorMarker.hasLowerNonNothingNonIltProperConstraint(): Boolean {
        val constraints = c.notFixedTypeVariables[this]?.constraints ?: return false

        return constraints.any { constraint ->
            // TODO: KT-82574 (it's strange that lower constraint is stronger than equals constraint here)
            constraint.kind.isLower()
                    && constraint.isProperArgumentConstraint()
                    && !constraint.type.typeConstructor().isNothingConstructor()
                    && !constraint.type.contains { it.typeConstructor().isIntegerLiteralTypeConstructor() }
        }
    }

    context(c: Context)
    override fun typeVariableHasProperConstraint(
        typeVariable: TypeConstructorMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider,
    ): Boolean {
        val readiness = typeVariable.getReadiness(dependencyProvider)
        return readiness[Q.ALLOWED] && readiness[Q.HAS_PROPER_CONSTRAINTS]
    }

    context(c: Context)
    override fun prepareVariableForFixation(
        candidate: TypeConstructorMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider
    ): VariableForFixation? {
        val readiness = candidate.getReadiness(dependencyProvider)

        return when {
            !readiness[Q.ALLOWED] -> null
            else -> VariableForFixation(
                variable = candidate,
                hasProperConstraint = readiness[Q.HAS_PROPER_CONSTRAINTS],
                hasDependencyOnOuterTypeVariable = !readiness[Q.HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY],
            )
        }
    }
}
