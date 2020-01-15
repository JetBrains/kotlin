/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls.inference.model

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.ResolutionCandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.ResolutionCandidateApplicability.INAPPLICABLE
import org.jetbrains.kotlin.resolve.calls.tower.ResolutionCandidateApplicability.INAPPLICABLE_WRONG_RECEIVER
import org.jetbrains.kotlin.resolve.scopes.receivers.DetailedReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker


sealed class ConstraintPosition

class ExplicitTypeParameterConstraintPosition(val typeArgument: SimpleTypeArgument) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString() = "TypeParameter $typeArgument"
}

class ExpectedTypeConstraintPosition(val topLevelCall: KotlinCall) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString() = "ExpectedType for call $topLevelCall"
}

sealed class DeclaredUpperBoundConstraintPosition : ConstraintPosition()

class DeclaredUpperBoundConstraintPositionImpl(val typeParameterDescriptor: TypeParameterDescriptor) : DeclaredUpperBoundConstraintPosition() {
    override fun toString() = "DeclaredUpperBound ${typeParameterDescriptor.name} from ${typeParameterDescriptor.containingDeclaration}"
}

class FirDeclaredUpperBoundConstraintPosition : DeclaredUpperBoundConstraintPosition()

interface OnlyInputTypeConstraintPosition

class ArgumentConstraintPosition(val argument: KotlinCallArgument) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString() = "Argument $argument"
}

class ReceiverConstraintPosition(val argument: KotlinCallArgument) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString() = "Receiver $argument"
}

class FixVariableConstraintPosition(val variable: TypeVariableMarker, val resolvedAtom: ResolvedAtom?) : ConstraintPosition() {
    override fun toString() = "Fix variable $variable"
}

class KnownTypeParameterConstraintPosition(val typeArgument: KotlinType) : ConstraintPosition() {
    override fun toString() = "TypeArgument $typeArgument"
}

class LHSArgumentConstraintPosition(
    val argument: CallableReferenceKotlinCallArgument,
    val receiver: DetailedReceiver
) : ConstraintPosition() {
    override fun toString(): String {
        return "LHS receiver $receiver"
    }
}

class LambdaArgumentConstraintPosition(val lambda: ResolvedLambdaAtom) : ConstraintPosition() {
    override fun toString(): String {
        return "LambdaArgument $lambda"
    }
}

class DelegatedPropertyConstraintPosition(val topLevelCall: KotlinCall) : ConstraintPosition() {
    override fun toString() = "Constraint from call $topLevelCall for delegated property"
}

class IncorporationConstraintPosition(
    val from: ConstraintPosition,
    val initialConstraint: InitialConstraint
) : ConstraintPosition() {
    override fun toString() =
        "Incorporate $initialConstraint from position $from"
}

class CoroutinePosition() : ConstraintPosition() {
    override fun toString(): String = "for coroutine call"
}

@Deprecated("Should be used only in SimpleConstraintSystemImpl")
object SimpleConstraintSystemConstraintPosition : ConstraintPosition()

abstract class ConstraintSystemCallDiagnostic(applicability: ResolutionCandidateApplicability) : KotlinCallDiagnostic(applicability) {
    override fun report(reporter: DiagnosticReporter) = reporter.constraintError(this)
}

class NewConstraintError(
    val lowerType: KotlinTypeMarker,
    val upperType: KotlinTypeMarker,
    val position: IncorporationConstraintPosition
) : ConstraintSystemCallDiagnostic(if (position.from is ReceiverConstraintPosition) INAPPLICABLE_WRONG_RECEIVER else INAPPLICABLE)

class CapturedTypeFromSubtyping(
    val typeVariable: TypeVariableMarker,
    val constraintType: KotlinTypeMarker,
    val position: ConstraintPosition
) : ConstraintSystemCallDiagnostic(INAPPLICABLE)

class NotEnoughInformationForTypeParameter(
    val typeVariable: TypeVariableMarker,
    val resolvedAtom: ResolvedAtom
) : ConstraintSystemCallDiagnostic(INAPPLICABLE)

class ConstrainingTypeIsError(
    val typeVariable: TypeVariableMarker,
    val constraintType: KotlinTypeMarker,
    val position: IncorporationConstraintPosition
) : ConstraintSystemCallDiagnostic(INAPPLICABLE)
