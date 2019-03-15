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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.components.*
import org.jetbrains.kotlin.resolve.calls.inference.addSubsystemFromArgument
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintInjector
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDynamicExtensionAnnotation
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.isDynamic


class KotlinCallComponents(
    val statelessCallbacks: KotlinResolutionStatelessCallbacks,
    val argumentsToParametersMapper: ArgumentsToParametersMapper,
    val typeArgumentsToParametersMapper: TypeArgumentsToParametersMapper,
    val constraintInjector: ConstraintInjector,
    val reflectionTypes: ReflectionTypes,
    val builtIns: KotlinBuiltIns,
    val languageVersionSettings: LanguageVersionSettings,
    val samConversionTransformer: SamConversionTransformer
)

class SimpleCandidateFactory(
    val callComponents: KotlinCallComponents,
    val scopeTower: ImplicitScopeTower,
    val kotlinCall: KotlinCall,
    val resolutionCallbacks: KotlinResolutionCallbacks
) : CandidateFactory<KotlinResolutionCandidate> {
    val inferenceSession: InferenceSession = resolutionCallbacks.inferenceSession

    val baseSystem: ConstraintStorage

    init {
        val baseSystem = NewConstraintSystemImpl(callComponents.constraintInjector, callComponents.builtIns)
        baseSystem.addSubsystemFromArgument(kotlinCall.explicitReceiver)
        baseSystem.addSubsystemFromArgument(kotlinCall.dispatchReceiverForInvokeExtension)
        for (argument in kotlinCall.argumentsInParenthesis) {
            baseSystem.addSubsystemFromArgument(argument)
        }
        baseSystem.addSubsystemFromArgument(kotlinCall.externalArgument)

        baseSystem.addOtherSystem(inferenceSession.currentConstraintSystem())

        this.baseSystem = baseSystem.asReadOnlyStorage()
    }

    // todo: try something else, because current method is ugly and unstable
    private fun createReceiverArgument(
        explicitReceiver: ReceiverKotlinCallArgument?,
        fromResolution: ReceiverValueWithSmartCastInfo?
    ): SimpleKotlinCallArgument? =
        explicitReceiver as? SimpleKotlinCallArgument ?: // qualifier receiver cannot be safe
        fromResolution?.let { ReceiverExpressionKotlinCallArgument(it, isSafeCall = false) } // todo smartcast implicit this

    private fun KotlinCall.getExplicitDispatchReceiver(explicitReceiverKind: ExplicitReceiverKind) = when (explicitReceiverKind) {
        ExplicitReceiverKind.DISPATCH_RECEIVER -> explicitReceiver
        ExplicitReceiverKind.BOTH_RECEIVERS -> dispatchReceiverForInvokeExtension
        else -> null
    }

    private fun KotlinCall.getExplicitExtensionReceiver(explicitReceiverKind: ExplicitReceiverKind) = when (explicitReceiverKind) {
        ExplicitReceiverKind.EXTENSION_RECEIVER, ExplicitReceiverKind.BOTH_RECEIVERS -> explicitReceiver
        else -> null
    }

    fun createCandidate(givenCandidate: GivenCandidate): KotlinResolutionCandidate {
        val isSafeCall = (kotlinCall.explicitReceiver as? SimpleKotlinCallArgument)?.isSafeCall ?: false

        val explicitReceiverKind =
            if (givenCandidate.dispatchReceiver == null) ExplicitReceiverKind.NO_EXPLICIT_RECEIVER else ExplicitReceiverKind.DISPATCH_RECEIVER
        val dispatchArgumentReceiver = givenCandidate.dispatchReceiver?.let { ReceiverExpressionKotlinCallArgument(it, isSafeCall) }
        return createCandidate(
            givenCandidate.descriptor, explicitReceiverKind, dispatchArgumentReceiver, null,
            listOf(), givenCandidate.knownTypeParametersResultingSubstitutor
        )
    }

    override fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): KotlinResolutionCandidate {
        val dispatchArgumentReceiver = createReceiverArgument(
            kotlinCall.getExplicitDispatchReceiver(explicitReceiverKind),
            towerCandidate.dispatchReceiver
        )
        val extensionArgumentReceiver =
            createReceiverArgument(kotlinCall.getExplicitExtensionReceiver(explicitReceiverKind), extensionReceiver)

        return createCandidate(
            towerCandidate.descriptor, explicitReceiverKind, dispatchArgumentReceiver,
            extensionArgumentReceiver, towerCandidate.diagnostics, knownSubstitutor = null
        )
    }

    private fun createCandidate(
        descriptor: CallableDescriptor,
        explicitReceiverKind: ExplicitReceiverKind,
        dispatchArgumentReceiver: SimpleKotlinCallArgument?,
        extensionArgumentReceiver: SimpleKotlinCallArgument?,
        initialDiagnostics: Collection<KotlinCallDiagnostic>,
        knownSubstitutor: TypeSubstitutor?
    ): KotlinResolutionCandidate {
        val resolvedKtCall = MutableResolvedCallAtom(
            kotlinCall, descriptor, explicitReceiverKind,
            dispatchArgumentReceiver, extensionArgumentReceiver
        )

        if (ErrorUtils.isError(descriptor)) {
            return KotlinResolutionCandidate(
                callComponents,
                scopeTower,
                baseSystem,
                resolvedKtCall,
                knownSubstitutor,
                listOf(ErrorDescriptorResolutionPart)
            )
        }

        val candidate = KotlinResolutionCandidate(callComponents, scopeTower, baseSystem, resolvedKtCall, knownSubstitutor)

        initialDiagnostics.forEach(candidate::addDiagnostic)

        if (callComponents.statelessCallbacks.isHiddenInResolution(descriptor, kotlinCall, resolutionCallbacks)) {
            candidate.addDiagnostic(HiddenDescriptor)
        }

        if (extensionArgumentReceiver != null) {
            val parameterIsDynamic = descriptor.extensionReceiverParameter!!.value.type.isDynamic()
            val argumentIsDynamic = extensionArgumentReceiver.receiver.receiverValue.type.isDynamic()

            if (parameterIsDynamic != argumentIsDynamic ||
                (parameterIsDynamic && !descriptor.hasDynamicExtensionAnnotation())) {
                candidate.addDiagnostic(HiddenExtensionRelatedToDynamicTypes)
            }
        }

        return candidate
    }

    fun createErrorCandidate(): KotlinResolutionCandidate {
        val errorScope = ErrorUtils.createErrorScope("Error resolution candidate for call $kotlinCall")
        val errorDescriptor = if (kotlinCall.callKind == KotlinCallKind.VARIABLE) {
            errorScope.getContributedVariables(kotlinCall.name, scopeTower.location)
        } else {
            errorScope.getContributedFunctions(kotlinCall.name, scopeTower.location)
        }.first()

        val dispatchReceiver = createReceiverArgument(kotlinCall.explicitReceiver, fromResolution = null)
        val explicitReceiverKind =
            if (dispatchReceiver == null) ExplicitReceiverKind.NO_EXPLICIT_RECEIVER else ExplicitReceiverKind.DISPATCH_RECEIVER

        return createCandidate(
            errorDescriptor, explicitReceiverKind, dispatchReceiver, extensionArgumentReceiver = null,
            initialDiagnostics = listOf(), knownSubstitutor = null
        )
    }

}

enum class KotlinCallKind(vararg resolutionPart: ResolutionPart) {
    VARIABLE(
        CheckVisibility,
        CheckInfixResolutionPart,
        CheckOperatorResolutionPart,
        CheckSuperExpressionCallPart,
        NoTypeArguments,
        NoArguments,
        CreateFreshVariablesSubstitutor,
        CheckExplicitReceiverKindConsistency,
        CheckReceivers,
        PostponedVariablesInitializerResolutionPart
    ),
    FUNCTION(
        CheckInstantiationOfAbstractClass,
        CheckVisibility,
        CheckInfixResolutionPart,
        CheckSuperExpressionCallPart,
        MapTypeArguments,
        MapArguments,
        ArgumentsToCandidateParameterDescriptor,
        CreateFreshVariablesSubstitutor,
        CheckExplicitReceiverKindConsistency,
        CheckReceivers,
        CheckArgumentsInParenthesis,
        CheckExternalArgument,
        PostponedVariablesInitializerResolutionPart
    ),
    INVOKE(*FUNCTION.resolutionSequence.toTypedArray()),
    UNSUPPORTED();

    val resolutionSequence = resolutionPart.asList()
}

class GivenCandidate(
    val descriptor: FunctionDescriptor,
    val dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    val knownTypeParametersResultingSubstitutor: TypeSubstitutor?
)

