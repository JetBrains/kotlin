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
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.components.candidate.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintError
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintWarning
import org.jetbrains.kotlin.resolve.calls.inference.model.transformToWarning
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability.*
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType

interface TransformableToWarning<T : KotlinCallDiagnostic> {
    fun transformToWarning(): T?
}

abstract class InapplicableArgumentDiagnostic : KotlinCallDiagnostic(INAPPLICABLE) {
    abstract val argument: KotlinCallArgument

    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}

abstract class CallableReferenceInapplicableDiagnostic(
    private val argument: CallableReferenceResolutionAtom,
    applicability: CandidateApplicability = INAPPLICABLE
) : KotlinCallDiagnostic(applicability) {
    override fun report(reporter: DiagnosticReporter) {
        when (argument) {
            is CallableReferenceKotlinCall -> reporter.onCall(this)
            is CallableReferenceKotlinCallArgument -> reporter.onCallArgument(argument, this)
        }
    }
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

object TypeCheckerHasRanIntoRecursion : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCall(this)
}

// Callable reference resolution
class CallableReferenceNotCompatible(
    argument: CallableReferenceResolutionAtom,
    val candidate: CallableMemberDescriptor,
    val expectedType: UnwrappedType?,
    val callableReverenceType: UnwrappedType
) : CallableReferenceInapplicableDiagnostic(argument)

// supported by FE but not supported by BE now
class CallableReferencesDefaultArgumentUsed(
    val argument: CallableReferenceResolutionAtom,
    val candidate: CallableDescriptor,
    val defaultsCount: Int
) : CallableReferenceInapplicableDiagnostic(argument)

class NotCallableMemberReference(
    val argument: CallableReferenceResolutionAtom,
    val candidate: CallableDescriptor
) : CallableReferenceInapplicableDiagnostic(argument)

class NoneCallableReferenceCallCandidates(val argument: CallableReferenceKotlinCallArgument) :
    CallableReferenceInapplicableDiagnostic(argument)

class CallableReferenceCallCandidatesAmbiguity(
    val argument: CallableReferenceKotlinCallArgument,
    val candidates: Collection<CallableReferenceResolutionCandidate>
) : CallableReferenceInapplicableDiagnostic(argument)

class NotCallableExpectedType(
    val argument: CallableReferenceKotlinCallArgument,
    val expectedType: UnwrappedType,
    val notCallableTypeConstructor: TypeConstructor
) : CallableReferenceInapplicableDiagnostic(argument)

class AdaptedCallableReferenceIsUsedWithReflection(val argument: CallableReferenceResolutionAtom) :
    CallableReferenceInapplicableDiagnostic(argument, RESOLVED_WITH_ERROR)

// SmartCasts
class SmartCastDiagnostic(
    val argument: ExpressionKotlinCallArgument,
    val smartCastType: UnwrappedType,
    val kotlinCall: KotlinCall?
) : KotlinCallDiagnostic(RESOLVED) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}

sealed class UnstableSmartCast(
    val argument: ExpressionKotlinCallArgument,
    val targetType: UnwrappedType,
    applicability: CandidateApplicability,
) : KotlinCallDiagnostic(applicability) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)

    companion object {
        operator fun invoke(
            argument: ExpressionKotlinCallArgument,
            targetType: UnwrappedType,
            @Suppress("UNUSED_PARAMETER") isReceiver: Boolean = false, // for reproducing OI behaviour
        ): UnstableSmartCast {
            return UnstableSmartCastResolutionError(argument, targetType)
        }
    }
}

class UnstableSmartCastResolutionError(
    argument: ExpressionKotlinCallArgument,
    targetType: UnwrappedType,
) : UnstableSmartCast(argument, targetType, UNSTABLE_SMARTCAST)

class UnstableSmartCastDiagnosticError(
    argument: ExpressionKotlinCallArgument,
    targetType: UnwrappedType,
) : UnstableSmartCast(argument, targetType, RESOLVED_WITH_ERROR)

class UnsafeCallError(
    val receiver: SimpleKotlinCallArgument,
    val isForImplicitInvoke: Boolean = false
) : KotlinCallDiagnostic(UNSAFE_CALL) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallReceiver(receiver, this)
}

// Other
object InstantiationOfAbstractClass : KotlinCallDiagnostic(RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCall(this)
}

class AbstractSuperCall(val receiver: SimpleKotlinCallArgument) : KotlinCallDiagnostic(RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

object AbstractFakeOverrideSuperCall : KotlinCallDiagnostic(RUNTIME_ERROR) {
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
class NoneCandidatesCallDiagnostic : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class ManyCandidatesCallDiagnostic(val candidates: Collection<ResolutionCandidate>) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class NonApplicableCallForBuilderInferenceDiagnostic(val kotlinCall: KotlinCall) : KotlinCallDiagnostic(CONVENTION_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

sealed interface ArgumentNullabilityMismatchDiagnostic {
    val expectedType: UnwrappedType
    val actualType: UnwrappedType
    val expressionArgument: ExpressionKotlinCallArgument
}

class ArgumentNullabilityErrorDiagnostic(
    override val expectedType: UnwrappedType,
    override val actualType: UnwrappedType,
    override val expressionArgument: ExpressionKotlinCallArgument
) : KotlinCallDiagnostic(UNSAFE_CALL), TransformableToWarning<ArgumentNullabilityWarningDiagnostic>, ArgumentNullabilityMismatchDiagnostic {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(expressionArgument, this)
    }

    override fun transformToWarning() = ArgumentNullabilityWarningDiagnostic(expectedType, actualType, expressionArgument)
}

class ArgumentNullabilityWarningDiagnostic(
    override val expectedType: UnwrappedType,
    override val actualType: UnwrappedType,
    override val expressionArgument: ExpressionKotlinCallArgument
) : KotlinCallDiagnostic(RESOLVED), ArgumentNullabilityMismatchDiagnostic {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(expressionArgument, this)
    }
}

class ResolvedToSamWithVarargDiagnostic(val argument: KotlinCallArgument) : KotlinCallDiagnostic(RESOLVED_TO_SAM_WITH_VARARG) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(argument, this)
    }
}

class NotEnoughInformationForLambdaParameter(
    val lambdaArgument: LambdaKotlinCallArgument,
    val parameterIndex: Int
) : KotlinCallDiagnostic(RESOLVED_WITH_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(lambdaArgument, this)
    }
}

class CandidateChosenUsingOverloadResolutionByLambdaAnnotation : KotlinCallDiagnostic(RESOLVED) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class CompatibilityWarning(val candidate: CallableDescriptor) : KotlinCallDiagnostic(RESOLVED) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class CompatibilityWarningOnArgument(
    val argument: KotlinCallArgument,
    val candidate: CallableDescriptor
) : KotlinCallDiagnostic(RESOLVED) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(argument, this)
    }
}

class NoContextReceiver(val receiverDescriptor: ReceiverParameterDescriptor) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class MultipleArgumentsApplicableForContextReceiver(val receiverDescriptor: ReceiverParameterDescriptor) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class KotlinConstraintSystemDiagnostic(
    val error: ConstraintSystemError
) : KotlinCallDiagnostic(error.applicability), TransformableToWarning<KotlinConstraintSystemDiagnostic> {
    override fun report(reporter: DiagnosticReporter) = reporter.constraintError(error)

    override fun transformToWarning(): KotlinConstraintSystemDiagnostic? =
        if (error is NewConstraintError) KotlinConstraintSystemDiagnostic(error.transformToWarning()) else null
}

val KotlinCallDiagnostic.constraintSystemError: ConstraintSystemError?
    get() = (this as? KotlinConstraintSystemDiagnostic)?.error

fun ConstraintSystemError.asDiagnostic(): KotlinConstraintSystemDiagnostic = KotlinConstraintSystemDiagnostic(this)
fun Collection<ConstraintSystemError>.asDiagnostics(): List<KotlinConstraintSystemDiagnostic> = map(ConstraintSystemError::asDiagnostic)

fun List<KotlinCallDiagnostic>.filterErrorDiagnostics() =
    filter { it !is KotlinConstraintSystemDiagnostic || it.error !is NewConstraintWarning }
