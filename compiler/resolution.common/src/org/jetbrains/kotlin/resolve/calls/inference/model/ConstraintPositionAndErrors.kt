/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.model

import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability.*
import org.jetbrains.kotlin.types.EmptyIntersectionTypeKind
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker

interface OnlyInputTypeConstraintPosition

sealed class ConstraintPosition

abstract class ExplicitTypeParameterConstraintPosition<T>(val typeArgument: T) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "TypeParameter $typeArgument"
}

abstract class InjectedAnotherStubTypeConstraintPosition<T>(private val builderInferenceLambdaOfInjectedStubType: T) : ConstraintPosition(),
    OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Injected from $builderInferenceLambdaOfInjectedStubType builder inference call"
}

abstract class BuilderInferenceSubstitutionConstraintPosition<L>(
    private val builderInferenceLambda: L,
    val initialConstraint: InitialConstraint,
    val isFromNotSubstitutedDeclaredUpperBound: Boolean = false
) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Incorporated builder inference constraint $initialConstraint " +
            "into $builderInferenceLambda call"
}

abstract class ExpectedTypeConstraintPosition<T>(val topLevelCall: T) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "ExpectedType for call $topLevelCall"
}

abstract class DeclaredUpperBoundConstraintPosition<T>(val typeParameter: T) : ConstraintPosition() {
    override fun toString(): String = "DeclaredUpperBound $typeParameter"
}

abstract class ArgumentConstraintPosition<out T>(val argument: T) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Argument $argument"
}

abstract class CallableReferenceConstraintPosition<out T>(val call: T) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Callable reference $call"
}

abstract class ReceiverConstraintPosition<T>(val argument: T) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Receiver $argument"
}

abstract class FixVariableConstraintPosition<T>(val variable: TypeVariableMarker, val resolvedAtom: T) : ConstraintPosition() {
    override fun toString(): String = "Fix variable $variable"
}

abstract class KnownTypeParameterConstraintPosition<T : KotlinTypeMarker>(val typeArgument: T) : ConstraintPosition() {
    override fun toString(): String = "TypeArgument $typeArgument"
}

abstract class LHSArgumentConstraintPosition<T, R>(
    val argument: T,
    val receiver: R
) : ConstraintPosition() {
    override fun toString(): String {
        return "LHS receiver $receiver"
    }
}

abstract class LambdaArgumentConstraintPosition<T>(val lambda: T) : ConstraintPosition() {
    override fun toString(): String {
        return "LambdaArgument $lambda"
    }
}

open class DelegatedPropertyConstraintPosition<T>(val topLevelCall: T) : ConstraintPosition() {
    override fun toString(): String = "Constraint from call $topLevelCall for delegated property"
}

data class IncorporationConstraintPosition(
    val initialConstraint: InitialConstraint,
    var isFromDeclaredUpperBound: Boolean = false
) : ConstraintPosition() {
    val from: ConstraintPosition get() = initialConstraint.position

    override fun toString(): String = "Incorporate $initialConstraint from position $from"
}

object BuilderInferencePosition : ConstraintPosition() {
    override fun toString(): String = "For builder inference call"
}

// TODO: should be used only in SimpleConstraintSystemImpl
object SimpleConstraintSystemConstraintPosition : ConstraintPosition()

// ------------------------------------------------ Errors ------------------------------------------------

sealed class ConstraintSystemError(val applicability: CandidateApplicability)

sealed interface NewConstraintMismatch {
    val lowerType: KotlinTypeMarker
    val upperType: KotlinTypeMarker
    val position: IncorporationConstraintPosition
}

class NewConstraintError(
    override val lowerType: KotlinTypeMarker,
    override val upperType: KotlinTypeMarker,
    override val position: IncorporationConstraintPosition,
) : ConstraintSystemError(if (position.from is ReceiverConstraintPosition<*>) INAPPLICABLE_WRONG_RECEIVER else INAPPLICABLE),
    NewConstraintMismatch {
    override fun toString(): String {
        return "$lowerType <: $upperType"
    }
}

class NewConstraintWarning(
    override val lowerType: KotlinTypeMarker,
    override val upperType: KotlinTypeMarker,
    override val position: IncorporationConstraintPosition,
) : ConstraintSystemError(RESOLVED), NewConstraintMismatch

class CapturedTypeFromSubtyping(
    val typeVariable: TypeVariableMarker,
    val constraintType: KotlinTypeMarker,
    val position: ConstraintPosition
) : ConstraintSystemError(INAPPLICABLE)

open class NotEnoughInformationForTypeParameter<T>(
    val typeVariable: TypeVariableMarker,
    val resolvedAtom: T,
    val couldBeResolvedWithUnrestrictedBuilderInference: Boolean
) : ConstraintSystemError(INAPPLICABLE)

class InferredIntoDeclaredUpperBounds(val typeVariable: TypeVariableMarker) : ConstraintSystemError(RESOLVED)

class ConstrainingTypeIsError(
    val typeVariable: TypeVariableMarker,
    val constraintType: KotlinTypeMarker,
    val position: IncorporationConstraintPosition
) : ConstraintSystemError(INAPPLICABLE)

class NoSuccessfulFork(val position: IncorporationConstraintPosition) : ConstraintSystemError(INAPPLICABLE)

sealed interface InferredEmptyIntersection {
    val incompatibleTypes: List<KotlinTypeMarker>
    val causingTypes: List<KotlinTypeMarker>
    val typeVariable: TypeVariableMarker
    val kind: EmptyIntersectionTypeKind
}

class InferredEmptyIntersectionWarning(
    override val incompatibleTypes: List<KotlinTypeMarker>,
    override val causingTypes: List<KotlinTypeMarker>,
    override val typeVariable: TypeVariableMarker,
    override val kind: EmptyIntersectionTypeKind,
) : ConstraintSystemError(RESOLVED), InferredEmptyIntersection

class InferredEmptyIntersectionError(
    override val incompatibleTypes: List<KotlinTypeMarker>,
    override val causingTypes: List<KotlinTypeMarker>,
    override val typeVariable: TypeVariableMarker,
    override val kind: EmptyIntersectionTypeKind,
) : ConstraintSystemError(INAPPLICABLE), InferredEmptyIntersection

class OnlyInputTypesDiagnostic(val typeVariable: TypeVariableMarker) : ConstraintSystemError(INAPPLICABLE)

class LowerPriorityToPreserveCompatibility(val needToReportWarning: Boolean) :
    ConstraintSystemError(RESOLVED_NEED_PRESERVE_COMPATIBILITY)

fun Constraint.isExpectedTypePosition() =
    position.from is ExpectedTypeConstraintPosition<*> || position.from is DelegatedPropertyConstraintPosition<*>

fun NewConstraintError.transformToWarning() = NewConstraintWarning(lowerType, upperType, position)
