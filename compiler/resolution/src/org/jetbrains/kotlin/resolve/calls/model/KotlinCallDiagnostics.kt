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

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.components.CallableReferenceCandidate
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.tower.ResolutionCandidateApplicability.*
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType

abstract class InapplicableArgumentDiagnostic : KotlinCallDiagnostic(INAPPLICABLE) {
    abstract val argument: KotlinCallArgument

    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}

// ArgumentsToParameterMapper
class TooManyArguments(val argument: KotlinCallArgument, val descriptor: CallableDescriptor) :
    KotlinCallDiagnostic(INAPPLICABLE_ARGUMENTS_MAPPING_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}

class NonVarargSpread(val argument: KotlinCallArgument) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentSpread(argument, this)
}

class MixingNamedAndPositionArguments(override val argument: KotlinCallArgument) : InapplicableArgumentDiagnostic()

class NamedArgumentNotAllowed(val argument: KotlinCallArgument, val descriptor: CallableDescriptor) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}

class NameNotFound(val argument: KotlinCallArgument, val descriptor: CallableDescriptor) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}

class NoValueForParameter(
    val parameterDescriptor: ValueParameterDescriptor,
    val descriptor: CallableDescriptor
) : KotlinCallDiagnostic(INAPPLICABLE_ARGUMENTS_MAPPING_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCall(this)
}

class ArgumentPassedTwice(
    val argument: KotlinCallArgument,
    val parameterDescriptor: ValueParameterDescriptor,
    val firstOccurrence: ResolvedCallArgument
) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}

class VarargArgumentOutsideParentheses(
    override val argument: KotlinCallArgument,
    val parameterDescriptor: ValueParameterDescriptor
) : InapplicableArgumentDiagnostic()

class NameForAmbiguousParameter(
    val argument: KotlinCallArgument,
    val parameterDescriptor: ValueParameterDescriptor,
    val overriddenParameterWithOtherName: ValueParameterDescriptor
) : KotlinCallDiagnostic(CONVENTION_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}

class NamedArgumentReference(
    val argument: KotlinCallArgument,
    val parameterDescriptor: ValueParameterDescriptor
) : KotlinCallDiagnostic(RESOLVED) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}

// TypeArgumentsToParameterMapper
class WrongCountOfTypeArguments(
    val descriptor: CallableDescriptor,
    val currentCount: Int
) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onTypeArguments(this)
}

// Callable reference resolution
class CallableReferenceNotCompatible(
    override val argument: CallableReferenceKotlinCallArgument,
    val candidate: CallableMemberDescriptor,
    val expectedType: UnwrappedType?,
    val callableReverenceType: UnwrappedType
) : InapplicableArgumentDiagnostic()

// supported by FE but not supported by BE now
class CallableReferencesDefaultArgumentUsed(
    val argument: CallableReferenceKotlinCallArgument,
    val candidate: CallableDescriptor,
    val defaultsCount: Int
) : KotlinCallDiagnostic(IMPOSSIBLE_TO_GENERATE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}

class NotCallableMemberReference(
    override val argument: CallableReferenceKotlinCallArgument,
    val candidate: CallableDescriptor
) : InapplicableArgumentDiagnostic()

class NoneCallableReferenceCandidates(override val argument: CallableReferenceKotlinCallArgument) : InapplicableArgumentDiagnostic()

class CallableReferenceCandidatesAmbiguity(
    override val argument: CallableReferenceKotlinCallArgument,
    val candidates: Collection<CallableReferenceCandidate>
) : InapplicableArgumentDiagnostic()

class NotCallableExpectedType(
    override val argument: CallableReferenceKotlinCallArgument,
    val expectedType: UnwrappedType,
    val notCallableTypeConstructor: TypeConstructor
) : InapplicableArgumentDiagnostic()

// SmartCasts
class SmartCastDiagnostic(
    val argument: ExpressionKotlinCallArgument,
    val smartCastType: UnwrappedType,
    val kotlinCall: KotlinCall?
) : KotlinCallDiagnostic(RESOLVED) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}

class UnstableSmartCast(
    val argument: ExpressionKotlinCallArgument,
    val targetType: UnwrappedType
) : KotlinCallDiagnostic(MAY_THROW_RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}

class UnsafeCallError(val receiver: SimpleKotlinCallArgument) : KotlinCallDiagnostic(MAY_THROW_RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallReceiver(receiver, this)
}

// Other
object InstantiationOfAbstractClass : KotlinCallDiagnostic(RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCall(this)
}

object AbstractSuperCall : KotlinCallDiagnostic(RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class SuperAsExtensionReceiver(val receiver: SimpleKotlinCallArgument) : KotlinCallDiagnostic(RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallReceiver(receiver, this)
    }
}

// candidates result
class NoneCandidatesCallDiagnostic(val kotlinCall: KotlinCall) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class ManyCandidatesCallDiagnostic(
    val kotlinCall: KotlinCall,
    val candidates: Collection<KotlinResolutionCandidate>
) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class NonApplicableCallForBuilderInferenceDiagnostic(val kotlinCall: KotlinCall) : KotlinCallDiagnostic(CONVENTION_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class ArgumentTypeMismatchDiagnostic(
    val expectedType: UnwrappedType,
    val actualType: UnwrappedType,
    val expressionArgument: ExpressionKotlinCallArgument
) : KotlinCallDiagnostic(MAY_THROW_RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(expressionArgument, this)
    }
}

class OnlyInputTypesDiagnostic(val typeVariable: NewTypeVariable) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}