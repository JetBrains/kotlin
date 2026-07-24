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

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
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
import org.jetbrains.kotlin.types.model.TypeParameterMarker

@K1Deprecation
interface TransformableToWarning<T : KotlinCallDiagnostic> {
    fun transformToWarning(): T?
}

@K1Deprecation
abstract class InapplicableArgumentDiagnostic : KotlinCallDiagnostic(INAPPLICABLE) {
    abstract val argument: KotlinCallArgument

    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}

@K1Deprecation
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
@K1Deprecation
class TooManyArguments(val argument: KotlinCallArgument, val descriptor: CallableDescriptor) :
    KotlinCallDiagnostic(INAPPLICABLE_ARGUMENTS_MAPPING_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}

@K1Deprecation
class NonVarargSpread(val argument: KotlinCallArgument) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentSpread(argument, this)
}

@K1Deprecation
class MultiLambdaBuilderInferenceRestriction(
    val argument: KotlinCallArgument,
    val typeParameter: TypeParameterMarker?
) : KotlinCallDiagnostic(RESOLVED) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}

@K1Deprecation
class StubBuilderInferenceReceiver(
    val receiver: SimpleKotlinCallArgument,
    val extensionReceiverParameter: ReceiverParameterDescriptor,
) : KotlinCallDiagnostic(RESOLVED) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallReceiver(receiver, this)
}

@K1Deprecation
class MixingNamedAndPositionArguments(override val argument: KotlinCallArgument) : InapplicableArgumentDiagnostic()

@K1Deprecation
class NamedArgumentNotAllowed(val argument: KotlinCallArgument, val descriptor: CallableDescriptor) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}

@K1Deprecation
class NameNotFound(val argument: KotlinCallArgument, val descriptor: CallableDescriptor) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}

@K1Deprecation
class NoValueForParameter(
    val parameterDescriptor: ValueParameterDescriptor,
    val descriptor: CallableDescriptor
) : KotlinCallDiagnostic(INAPPLICABLE_ARGUMENTS_MAPPING_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCall(this)
}

@K1Deprecation
class ArgumentPassedTwice(
    val argument: KotlinCallArgument,
    val parameterDescriptor: ValueParameterDescriptor,
    val firstOccurrence: ResolvedCallArgument
) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}

@K1Deprecation
class VarargArgumentOutsideParentheses(
    override val argument: KotlinCallArgument,
    val parameterDescriptor: ValueParameterDescriptor
) : InapplicableArgumentDiagnostic()

@K1Deprecation
class NameForAmbiguousParameter(
    val argument: KotlinCallArgument,
    val parameterDescriptor: ValueParameterDescriptor,
    val overriddenParameterWithOtherName: ValueParameterDescriptor
) : KotlinCallDiagnostic(CONVENTION_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}

@K1Deprecation
class NamedArgumentReference(
    val argument: KotlinCallArgument,
    val parameterDescriptor: ValueParameterDescriptor
) : KotlinCallDiagnostic(RESOLVED) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}

// TypeArgumentsToParameterMapper
@K1Deprecation
class WrongCountOfTypeArguments(
    val descriptor: CallableDescriptor,
    val currentCount: Int
) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onTypeArguments(this)
}

@K1Deprecation
object TypeCheckerHasRanIntoRecursion : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCall(this)
}

// supported by FE but not supported by BE now
@K1Deprecation
class CallableReferencesDefaultArgumentUsed(
    val argument: CallableReferenceResolutionAtom,
    val candidate: CallableDescriptor,
    val defaultsCount: Int
) : CallableReferenceInapplicableDiagnostic(argument)

@K1Deprecation
class NotCallableMemberReference(
    val argument: CallableReferenceResolutionAtom,
    val candidate: CallableDescriptor
) : CallableReferenceInapplicableDiagnostic(argument)

@K1Deprecation
class NoneCallableReferenceCallCandidates(val argument: CallableReferenceKotlinCallArgument) :
    CallableReferenceInapplicableDiagnostic(argument)

@K1Deprecation
class CallableReferenceCallCandidatesAmbiguity(
    val argument: CallableReferenceKotlinCallArgument,
    val candidates: Collection<CallableReferenceResolutionCandidate>
) : CallableReferenceInapplicableDiagnostic(argument)

@K1Deprecation
class NotCallableExpectedType(
    val argument: CallableReferenceKotlinCallArgument,
    val expectedType: UnwrappedType,
    val notCallableTypeConstructor: TypeConstructor
) : CallableReferenceInapplicableDiagnostic(argument)

@K1Deprecation
class AdaptedCallableReferenceIsUsedWithReflection(val argument: CallableReferenceResolutionAtom) :
    CallableReferenceInapplicableDiagnostic(argument, RESOLVED_WITH_ERROR)

// SmartCasts
@K1Deprecation
class SmartCastDiagnostic(
    val argument: ExpressionKotlinCallArgument,
    val smartCastType: UnwrappedType,
    val kotlinCall: KotlinCall?
) : KotlinCallDiagnostic(RESOLVED) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}

@K1Deprecation
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

@K1Deprecation
class UnstableSmartCastResolutionError(
    argument: ExpressionKotlinCallArgument,
    targetType: UnwrappedType,
) : UnstableSmartCast(argument, targetType, UNSTABLE_SMARTCAST)

@K1Deprecation
class UnstableSmartCastDiagnosticError(
    argument: ExpressionKotlinCallArgument,
    targetType: UnwrappedType,
) : UnstableSmartCast(argument, targetType, RESOLVED_WITH_ERROR)

@K1Deprecation
class UnsafeCallError(
    val receiver: SimpleKotlinCallArgument,
    val isForImplicitInvoke: Boolean = false
) : KotlinCallDiagnostic(UNSAFE_CALL) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallReceiver(receiver, this)
}

// Other
@K1Deprecation
object InstantiationOfAbstractClass : KotlinCallDiagnostic(K1_RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCall(this)
}

@K1Deprecation
class AbstractSuperCall(val receiver: SimpleKotlinCallArgument) : KotlinCallDiagnostic(K1_RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

@K1Deprecation
object AbstractFakeOverrideSuperCall : KotlinCallDiagnostic(K1_RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

@K1Deprecation
class SuperAsExtensionReceiver(val receiver: SimpleKotlinCallArgument) : KotlinCallDiagnostic(K1_RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallReceiver(receiver, this)
    }
}

// candidates result
@K1Deprecation
class NoneCandidatesCallDiagnostic : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

@K1Deprecation
class ManyCandidatesCallDiagnostic(val candidates: Collection<ResolutionCandidate>) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

@K1Deprecation
class NonApplicableCallForBuilderInferenceDiagnostic(val kotlinCall: KotlinCall) : KotlinCallDiagnostic(CONVENTION_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

@K1Deprecation
sealed interface ArgumentNullabilityMismatchDiagnostic {
    val expectedType: UnwrappedType
    val actualType: UnwrappedType
    val expressionArgument: ExpressionKotlinCallArgument
}

@K1Deprecation
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

@K1Deprecation
class ArgumentNullabilityWarningDiagnostic(
    override val expectedType: UnwrappedType,
    override val actualType: UnwrappedType,
    override val expressionArgument: ExpressionKotlinCallArgument
) : KotlinCallDiagnostic(RESOLVED), ArgumentNullabilityMismatchDiagnostic {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(expressionArgument, this)
    }
}

@K1Deprecation
class ResolvedToSamWithVarargDiagnostic(val argument: KotlinCallArgument) : KotlinCallDiagnostic(K1_RESOLVED_TO_SAM_WITH_VARARG) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(argument, this)
    }
}

@K1Deprecation
class NotEnoughInformationForLambdaParameter(
    val lambdaArgument: LambdaKotlinCallArgument,
    val parameterIndex: Int
) : KotlinCallDiagnostic(RESOLVED_WITH_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(lambdaArgument, this)
    }
}

@K1Deprecation
class CandidateChosenUsingOverloadResolutionByLambdaAnnotation : KotlinCallDiagnostic(RESOLVED) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

@K1Deprecation
class EnumEntryAmbiguityWarning(val property: PropertyDescriptor, val enumEntry: ClassDescriptor) : KotlinCallDiagnostic(RESOLVED) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

@K1Deprecation
class CompatibilityWarning(val candidate: CallableDescriptor) : KotlinCallDiagnostic(RESOLVED) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

@K1Deprecation
class CompatibilityWarningOnArgument(
    val argument: KotlinCallArgument,
    val candidate: CallableDescriptor
) : KotlinCallDiagnostic(RESOLVED) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(argument, this)
    }
}

@K1Deprecation
class NoContextReceiver(val receiverDescriptor: ReceiverParameterDescriptor) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

@K1Deprecation
class MultipleArgumentsApplicableForContextReceiver(val receiverDescriptor: ReceiverParameterDescriptor) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

@K1Deprecation
class KotlinConstraintSystemDiagnostic(
    val error: ConstraintSystemError
) : KotlinCallDiagnostic(error.applicability), TransformableToWarning<KotlinConstraintSystemDiagnostic> {
    override fun report(reporter: DiagnosticReporter) = reporter.constraintError(error)

    override fun transformToWarning(): KotlinConstraintSystemDiagnostic? =
        if (error is NewConstraintError) KotlinConstraintSystemDiagnostic(error.transformToWarning()) else null
}

@K1Deprecation
val KotlinCallDiagnostic.constraintSystemError: ConstraintSystemError?
    get() = (this as? KotlinConstraintSystemDiagnostic)?.error

@K1Deprecation
fun ConstraintSystemError.asDiagnostic(): KotlinConstraintSystemDiagnostic = KotlinConstraintSystemDiagnostic(this)
@K1Deprecation
fun Collection<ConstraintSystemError>.asDiagnostics(): List<KotlinConstraintSystemDiagnostic> = map(ConstraintSystemError::asDiagnostic)

@K1Deprecation
fun List<KotlinCallDiagnostic>.filterErrorDiagnostics() =
    filter { it !is KotlinConstraintSystemDiagnostic || it.error !is NewConstraintWarning }
