/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.model

import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability.*
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker

interface OnlyInputTypeConstraintPosition

sealed class ConstraintPosition

abstract class ExplicitTypeParameterConstraintPosition<T>(val typeArgument: T) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "TypeParameter $typeArgument"
}

abstract class ExpectedTypeConstraintPosition<T>(val topLevelCall: T) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "ExpectedType for call $topLevelCall"
}

abstract class DeclaredUpperBoundConstraintPosition<T>(val typeParameter: T) : ConstraintPosition() {
    override fun toString(): String = "DeclaredUpperBound $typeParameter"
}

abstract class ArgumentConstraintPosition<T>(val argument: T) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Argument $argument"
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

abstract class DelegatedPropertyConstraintPosition<T>(val topLevelCall: T) : ConstraintPosition() {
    override fun toString(): String = "Constraint from call $topLevelCall for delegated property"
}

data class IncorporationConstraintPosition(
    val from: ConstraintPosition,
    val initialConstraint: InitialConstraint,
    var isFromDeclaredUpperBound: Boolean = false
) : ConstraintPosition() {
    override fun toString(): String = "Incorporate $initialConstraint from position $from"
}

object CoroutinePosition : ConstraintPosition() {
    override fun toString(): String = "for coroutine call"
}

// TODO: should be used only in SimpleConstraintSystemImpl
object SimpleConstraintSystemConstraintPosition : ConstraintPosition()

// ------------------------------------------------ Errors ------------------------------------------------

sealed class ConstraintSystemError(val applicability: CandidateApplicability)

class NewConstraintError(
    val lowerType: KotlinTypeMarker,
    val upperType: KotlinTypeMarker,
    val position: IncorporationConstraintPosition
) : ConstraintSystemError(if (position.from is ReceiverConstraintPosition<*>) INAPPLICABLE_WRONG_RECEIVER else INAPPLICABLE)

class CapturedTypeFromSubtyping(
    val typeVariable: TypeVariableMarker,
    val constraintType: KotlinTypeMarker,
    val position: ConstraintPosition
) : ConstraintSystemError(INAPPLICABLE)

abstract class NotEnoughInformationForTypeParameter<T>(
    val typeVariable: TypeVariableMarker,
    val resolvedAtom: T
) : ConstraintSystemError(INAPPLICABLE)

class ConstrainingTypeIsError(
    val typeVariable: TypeVariableMarker,
    val constraintType: KotlinTypeMarker,
    val position: IncorporationConstraintPosition
) : ConstraintSystemError(INAPPLICABLE)

class OnlyInputTypesDiagnostic(val typeVariable: TypeVariableMarker) : ConstraintSystemError(INAPPLICABLE)

object LowerPriorityToPreserveCompatibility : ConstraintSystemError(RESOLVED_NEED_PRESERVE_COMPATIBILITY)
