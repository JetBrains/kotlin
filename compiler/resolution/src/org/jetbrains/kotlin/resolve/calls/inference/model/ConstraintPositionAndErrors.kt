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
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.UnwrappedType


sealed class ConstraintPosition

class ExplicitTypeParameterConstraintPosition(val typeArgument: SimpleTypeArgument) : ConstraintPosition() {
    override fun toString() = "TypeParameter $typeArgument"
}
class ExpectedTypeConstraintPosition(val topLevelCall: KotlinCall) : ConstraintPosition() {
    override fun toString() = "ExpectedType for call $topLevelCall"
}
class DeclaredUpperBoundConstraintPosition(val typeParameterDescriptor: TypeParameterDescriptor) : ConstraintPosition() {
    override fun toString() = "DeclaredUpperBound ${typeParameterDescriptor.name} from ${typeParameterDescriptor.containingDeclaration}"
}
class ArgumentConstraintPosition(val argument: KotlinCallArgument) : ConstraintPosition() {
    override fun toString() = "Argument $argument"
}
class FixVariableConstraintPosition(val variable: NewTypeVariable) : ConstraintPosition() {
    override fun toString() = "Fix variable $variable"
}
class KnownTypeParameterConstraintPosition(val typeArgument: KotlinType) : ConstraintPosition() {
    override fun toString() = "TypeArgument $typeArgument"
}

class IncorporationConstraintPosition(val from: ConstraintPosition, val initialConstraint: InitialConstraint) : ConstraintPosition() {
    override fun toString() = "Incorporate $initialConstraint from position $from"
}

@Deprecated("Should be used only in SimpleConstraintSystemImpl")
object SimpleConstraintSystemConstraintPosition : ConstraintPosition()


class NewConstraintError(val lowerType: UnwrappedType, val upperType: UnwrappedType, val position: IncorporationConstraintPosition):
        KotlinCallDiagnostic(ResolutionCandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.constraintError(this)
}

class CapturedTypeFromSubtyping(val typeVariable: NewTypeVariable, val constraintType: UnwrappedType, val position: ConstraintPosition) :
        KotlinCallDiagnostic(ResolutionCandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.constraintError(this)
}

class NotEnoughInformationForTypeParameter(val typeVariable: NewTypeVariable) : KotlinCallDiagnostic(ResolutionCandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.constraintError(this)
}