/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableFixationFinder.Context
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableFixationFinder.VariableForFixation
import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableReadinessCalculator.TypeVariableFixationReadinessQuality as Q

class VariableReadinessCalculator(
    trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    languageVersionSettings: LanguageVersionSettings,
    private val typeApproximator: AbstractTypeApproximator,
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

        // *** A prioritizer needed for KT-74999 (the Traversable vs. Path choice) ***
        // Currently, only used in 2.2+, and helps with self-type based declared upper bounds in particular situations.
        // Captured types are difficult to manipulate, so with `T <: Captured(...)` AND `T <: K` it's better to fix `T`
        // earlier than `K :> SomeRegularType` / `K <: SomeRegularType`, as otherwise, we will have
        // `T <: Captured(...) & SomeRegularType`, which is often problematic.
        // TODO: it would be probably better to use READY_FOR_FIXATION_UPPER here and to have
        //  it prioritized in comparison with READY_FOR_FIXATION_LOWER (however, KT-41934 example currently prevents it)
        HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES,

        // *** The following block constitutes what "ready for fixation" used to mean in the old fixation code ***
        HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT,
        HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES,

        // A proper trivial constraint from arguments, except for `T >: Nothing(?)`
        // It does however include `T = Nothing(?)` and `T <: Nothing(?)`
        HAS_PROPER_NON_TRIVIAL_CONSTRAINTS,
        HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND,

        // If a variable is based on a reified type parameter.
        // Generally, we shouldn't care much if a type variable is based on a reified type parameter, but it tends to be the most
        // common case for a materialize-like functions, having a shape like `fun <reified T> materialize(): T`.
        //
        // And for the usages like `val x: Base = run { materialize() ?: derived }`, it might be very important to infer `T` into `Base`.
        // That's why we prefer to fix such variables as soon as they have any proper (likely UPPER) constraints.
        //
        // One might notice that such a behavior might be reasonable for non-reified's as well, but it's more of the issue for KT-81083.
        // So, basically this readiness is only necessary to preserve backwards compatibility (see KT-86728).
        // Relevant test data: `reifiedFirst.kt` and `earlierReifiedFix.kt`
        // TODO: Consider getting rid of it (KT-81083)
        REIFIED,

        // Starts fixation of variables with a LOWER flexible constraint even if
        // they have LOWER constraints referring to some others: this makes fixing
        // them into the flexible type rather than a potential nullable counterpart
        // coming from fixing those other variables more likely.
        // This bit supersedes the default EQUALITY > LOWER prioritization to support
        // a single Java-related edge case.
        // See: `javaFunctionParamNullability.kt`, KT-82574.
        // TODO: KT-84257.
        HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT,

        // ILT type = Integer literal type = yet unknown choice from Byte/Short/Int/Long (at least two of them)
        // Any proper constraint not bound to an ILT type can be here.
        // See: `greater.kt`, KT-67335.
        HAS_PROPER_NON_ILT_CONSTRAINT,

        // Explicit lower `Nothing` constraints tend to always fix to `Nothing`
        // (if there's an explicit one, then the user wrote it on purpose somewhere),
        // which may "poison" other type variables depending on the current one.
        // This entry de-prioritizes variables that have `:> Nothing(?)` constraints
        // (both nullable and not null).
        // See: `reifiedToNothing.kt` (KT-76443) and `lambdaParameterTypeInElvis.kt`.
        HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT,

        // An explicit prioritizer of `EQUALITY` constraints that guarantees
        // they take precedence over both `LOWER` and `UPPER` constraints.
        // See: `flatMapWithReverseOrder.kt`, KT-71854.
        HAS_PROPER_EQUALITY_CONSTRAINT,

        // Prefer `LOWER` `T :> SomeRegularType` to `UPPER` `T <: SomeRegularType`
        // because `LOWER` constraints tend to lead to more specific fixations.
        // See `preferLowerToUpperConstraint.kt`, KT-41934.
        HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT,
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
                || dependencyProvider.isRelatedToCollectionLiteral(this)
        val areAllProperConstraintsSelfTypeBased = areAllProperConstraintsSelfTypeBased()

        // These values go in the same order as they are defined in `TypeVariableFixationReadinessQuality`,
        // except for being reversed: so that higher-priority ones come first.

        readiness[Q.ALLOWED] = !forbidden
        if (forbidden) return readiness

        val constraints = c.notFixedTypeVariables.getValue(this).constraints

        readiness[Q.HAS_PROPER_CONSTRAINTS] = hasProperArgumentConstraints() || areAllProperConstraintsSelfTypeBased
        readiness[Q.HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY] = !dependencyProvider.isRelatedToOuterTypeVariable(this)

        readiness[Q.HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES] = areAllProperConstraintsSelfTypeBased
                && fixationEnhancementsIn22
                && !isReified() && hasDirectConstraintToNotFixedRelevantVariable()

        readiness[Q.HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT] =
            readiness[Q.HAS_PROPER_CONSTRAINTS] && !areAllProperConstraintsSelfTypeBased
        readiness[Q.HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES] = !hasDependencyToOtherTypeVariables() &&
                !hasDependencyToOtherTypeVariablesViaSelfTypeAndShallowConstraint()
        readiness[Q.HAS_PROPER_NON_TRIVIAL_CONSTRAINTS] = !allConstraintsTrivialOrNonProper()
        readiness[Q.HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND] =
            !hasOnlyIncorporatedConstraintsFromDeclaredUpperBound()

        readiness[Q.HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT] = constraints
            .any { it.kind.isLower() && it.isProperArgumentConstraint() && it.type.isFlexible() }

        (val hasProperNonIltConstraint) = computeIltConstraintsRelatedFlags()
        readiness[Q.REIFIED] = isReified()
        readiness[Q.HAS_PROPER_NON_ILT_CONSTRAINT] = hasProperNonIltConstraint

        readiness[Q.HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT] = hasNoExplicitLowerNothingConstraint()

        readiness[Q.HAS_PROPER_EQUALITY_CONSTRAINT] = constraints
            .any { it.kind.isEqual() && it.isProperArgumentConstraint() }
        readiness[Q.HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT] = constraints
            .any { !it.kind.isUpper() && it.isProperArgumentConstraint() && !it.type.typeConstructor().isNothingConstructor() }

        return readiness
    }

    context(c: Context)
    private fun TypeConstructorMarker.hasNoExplicitLowerNothingConstraint(): Boolean =
        c.notFixedTypeVariables[this]?.constraints
            ?.none { it.kind.isLower() && it.type.typeConstructor().isNothingConstructor() }
            ?: true

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

    private val eliminateSecondKindIncorporation: Boolean =
        languageVersionSettings.supportsFeature(LanguageFeature.EliminateSecondKindIncorporation)

    /**
     * Models the fixation-order effect of the second incorporation kind without materializing its constraints
     * (see [LanguageFeature.EliminateSecondKindIncorporation], KT-86022).
     *
     * When the second kind ran eagerly, a constraint with a nested self-occurrence like `T <: Comparable<T>` combined with
     * a "shallow" constraint to another type variable (`S <: T`) materialized `T <: Comparable<S>`,
     * making [hasDependencyToOtherTypeVariables] true for `T`. Constraints without a nested self-occurrence don't need
     * this treatment: they either have another type variable in a nested position already (so [hasDependencyToOtherTypeVariables] is true)
     * or they don't but in this case the second-kind-incorporation would be a no-op.
     *
     * Note that the eager incorporation used to drop the projected results of invariant positions
     * (`containsConstrainingTypeWithoutProjection` requires an unprojected occurrence for non-EQUALITY causes)
     * unless the nullability rescue applied (`isPotentialUsefulNullabilityConstraint`); that filtering is considered
     * an implementation quirk and is deliberately not reproduced here.
     */
    context(c: Context)
    private fun TypeConstructorMarker.hasDependencyToOtherTypeVariablesViaSelfTypeAndShallowConstraint(): Boolean {
        if (!eliminateSecondKindIncorporation) return false
        val variableWithConstraints = c.notFixedTypeVariables[this] ?: return false
        // Only nested self-occurrences (inside a type argument) matter: for a top-level one (`T?`, `T & Any`, …)
        // the second kind used to produce either a trivial constraint or one covered by the direct incorporation
        val selfReferredConstraints = variableWithConstraints.getConstraintsContainedSpecifiedTypeVariable(this)
            .filter { it.type.lowerBoundIfFlexible().argumentsCount() != 0 }
            .ifEmpty { return false }

        var hasUpperShallowNeighbor = false
        var hasLowerShallowNeighbor = false

        for (constraint in variableWithConstraints.constraints) {
            val rigidType = constraint.type.lowerBoundIfFlexible()
            val neighbor = rigidType.originalIfDefinitelyNotNullable().typeConstructor()

            // Likely impossible, but not sure about red code
            if (neighbor == this) continue

            if (c.notFixedTypeVariables.containsKey(neighbor)) {
                when (constraint.kind) {
                    // An EQUALITY bound is substituted without approximation, so any self-referring constraint retains it
                    ConstraintKind.EQUALITY -> return true
                    ConstraintKind.UPPER -> hasUpperShallowNeighbor = true
                    ConstraintKind.LOWER -> hasLowerShallowNeighbor = true
                }
            }
        }

        if (!hasUpperShallowNeighbor && !hasLowerShallowNeighbor) return false

        // Just a random type which should survive after approximation as a sign that in the presence of the shallow neighbors
        // the type variable should be treated as having a complex dependency.
        // NB: This type doesn't leak outside of this function
        val stub = c.createStubTypeForTypeVariablesInSubtyping(variableWithConstraints.typeVariable)

        // The idea here is that for T <: SomeSelfType<T> and for example, if some T <: S exists, T should be treated as having complex dependency
        // whenever the approximation of SomeSelfType<Captured(out myStub)> would contain `myStub`.
        // In other words, it's possible to prove that T <: SomeSelfType<out S> is a correct constraint from the preconditions:
        //   T <: SomeSelfType<T> && T <: S
        //    => SomeSelfType<T> <: SomeSelfType<out S>
        //    => T <: SomeSelfType<out S>
        //
        // Or, speaking differently again, T <: SomeSelfType<out S> would be introduced during second kind of incorporation when it existed.
        // Basically, it is a simplified version of ConstraintIncorporator.computeConstraintTypeForSecondIncorporationKind
        if (hasUpperShallowNeighbor) {
            // Captured(out Stub(T))
            val capturedSubstitution = c.createCapturedType(
                c.createTypeArgument(stub, TypeVariance.OUT),
                constructorSupertypes = listOf(stub),
                lowerType = null,
                CaptureStatus.FOR_INCORPORATION,
            )

            anyApproximatedConstraintHasStubTypeSurvived(
                selfReferredConstraints,
                typeVariable = this,
                capturedSubstitution,
                stub
            ).ifTrue { return true }
        }

        if (hasLowerShallowNeighbor) {
            // Captured(in Stub(T))
            val capturedSubstitution = c.createCapturedType(
                c.createTypeArgument(stub, TypeVariance.IN),
                constructorSupertypes = emptyList(),
                lowerType = stub,
                CaptureStatus.FOR_INCORPORATION,
            )

            anyApproximatedConstraintHasStubTypeSurvived(
                selfReferredConstraints,
                typeVariable = this,
                capturedSubstitution,
                stub
            ).ifTrue { return true }
        }

        return false
    }

    context(c: Context)
    private fun anyApproximatedConstraintHasStubTypeSurvived(
        selfReferredConstraints: Collection<Constraint>,
        typeVariable: TypeConstructorMarker,
        capturedSubstitution: CapturedTypeMarker,
        stub: StubTypeMarker,
    ): Boolean {
        val stubConstructor = stub.typeConstructor()
        val substitutor = c.typeSubstitutorByTypeConstructor(
            mapOf(typeVariable to capturedSubstitution)
        )
        for (constraint in selfReferredConstraints) {
            val substitutedType = substitutor.safeSubstitute(constraint.type)
            check(substitutedType !== constraint.type) {
                "Substitution should not non-trivial for self-referred constraint $constraint"
            }

            // The approximation direction mirrors the second-kind incorporation: an UPPER containing constraint
            // (`T <: C<…>`) was approximated to a supertype, a LOWER one — to a subtype, an EQUALITY one — both ways
            if (constraint.kind.impliesUpper() &&
                stubSurvivesSurvivesCapturedTypeApproximation(substitutedType, toSuper = true, stubConstructor)
            ) {
                return true
            }
            if (constraint.kind.impliesLower() &&
                stubSurvivesSurvivesCapturedTypeApproximation(substitutedType, toSuper = false, stubConstructor)
            ) {
                return true
            }
        }
        return false
    }

    context(c: Context)
    private fun stubSurvivesSurvivesCapturedTypeApproximation(
        type: KotlinTypeMarker,
        toSuper: Boolean,
        stubConstructor: TypeConstructorMarker,
    ): Boolean {
        val approximatedType = when {
            toSuper -> typeApproximator.approximateToSuperType(
                type, TypeApproximatorConfiguration.IncorporationConfiguration,
                c.approximatorCaches,
            )
            else -> typeApproximator.approximateToSubType(
                type, TypeApproximatorConfiguration.IncorporationConfiguration,
                c.approximatorCaches,
            )
        } ?: type
        return approximatedType.contains { it.typeConstructor() === stubConstructor }
    }
}
