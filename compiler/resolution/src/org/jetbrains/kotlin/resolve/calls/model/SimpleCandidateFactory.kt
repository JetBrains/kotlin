/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.components.InferenceSession
import org.jetbrains.kotlin.resolve.calls.components.KotlinResolutionCallbacks
import org.jetbrains.kotlin.resolve.calls.components.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.components.candidate.SimpleErrorResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.components.candidate.SimpleResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.inference.addSubsystemFromArgument
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDynamicExtensionAnnotation
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.error.ErrorScopeKind
import org.jetbrains.kotlin.types.isDynamic

class SimpleCandidateFactory(
    val callComponents: KotlinCallComponents,
    val scopeTower: ImplicitScopeTower,
    val kotlinCall: KotlinCall,
    val resolutionCallbacks: KotlinResolutionCallbacks,
) : CandidateFactory<SimpleResolutionCandidate> {
    val inferenceSession: InferenceSession = resolutionCallbacks.inferenceSession

    val baseSystem: ConstraintStorage

    init {
        val baseSystem = NewConstraintSystemImpl(callComponents.constraintInjector, callComponents.builtIns, callComponents.kotlinTypeRefiner)
        if (!inferenceSession.resolveReceiverIndependently()) {
            baseSystem.addSubsystemFromArgument(kotlinCall.explicitReceiver)
            baseSystem.addSubsystemFromArgument(kotlinCall.dispatchReceiverForInvokeExtension)
        }
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
        fromResolution?.let {
            ReceiverExpressionKotlinCallArgument(it, isSafeCall = false, isForImplicitInvoke = kotlinCall.isForImplicitInvoke)
        } // todo smartcast implicit this

    private fun KotlinCall.getExplicitDispatchReceiver(explicitReceiverKind: ExplicitReceiverKind) = when (explicitReceiverKind) {
        ExplicitReceiverKind.DISPATCH_RECEIVER -> explicitReceiver
        ExplicitReceiverKind.BOTH_RECEIVERS -> dispatchReceiverForInvokeExtension
        else -> null
    }

    private fun KotlinCall.getExplicitExtensionReceiver(explicitReceiverKind: ExplicitReceiverKind) = when (explicitReceiverKind) {
        ExplicitReceiverKind.EXTENSION_RECEIVER, ExplicitReceiverKind.BOTH_RECEIVERS -> explicitReceiver
        else -> null
    }

    fun createCandidate(givenCandidate: GivenCandidate): SimpleResolutionCandidate {
        val isSafeCall = (kotlinCall.explicitReceiver as? SimpleKotlinCallArgument)?.isSafeCall ?: false

        val explicitReceiverKind =
            if (givenCandidate.dispatchReceiver == null) ExplicitReceiverKind.NO_EXPLICIT_RECEIVER else ExplicitReceiverKind.DISPATCH_RECEIVER
        val dispatchArgumentReceiver = givenCandidate.dispatchReceiver?.let {
            ReceiverExpressionKotlinCallArgument(it, isSafeCall)
        }
        return createCandidate(
            givenCandidate.descriptor, explicitReceiverKind, dispatchArgumentReceiver, null, null,
            listOf(), givenCandidate.knownTypeParametersResultingSubstitutor
        )
    }

    override fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): SimpleResolutionCandidate {
        val dispatchArgumentReceiver = createReceiverArgument(
            kotlinCall.getExplicitDispatchReceiver(explicitReceiverKind),
            towerCandidate.dispatchReceiver
        )
        val extensionArgumentReceiver =
            createReceiverArgument(kotlinCall.getExplicitExtensionReceiver(explicitReceiverKind), extensionReceiver)

        return createCandidate(
            towerCandidate.descriptor, explicitReceiverKind, dispatchArgumentReceiver,
            extensionArgumentReceiver, null, towerCandidate.diagnostics, knownSubstitutor = null
        )
    }

    override fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind,
        extensionReceiverCandidates: List<ReceiverValueWithSmartCastInfo>
    ): SimpleResolutionCandidate {
        val dispatchArgumentReceiver = createReceiverArgument(
            kotlinCall.getExplicitDispatchReceiver(explicitReceiverKind),
            towerCandidate.dispatchReceiver
        )
        val extensionArgumentReceiverCandidates = extensionReceiverCandidates.mapNotNull {
            createReceiverArgument(kotlinCall.getExplicitExtensionReceiver(explicitReceiverKind), it)
        }

        return createCandidate(
            towerCandidate.descriptor, explicitReceiverKind, dispatchArgumentReceiver,
            null, extensionArgumentReceiverCandidates, towerCandidate.diagnostics, knownSubstitutor = null
        )
    }

    private fun createCandidate(
        descriptor: CallableDescriptor,
        explicitReceiverKind: ExplicitReceiverKind,
        dispatchArgumentReceiver: SimpleKotlinCallArgument?,
        extensionArgumentReceiver: SimpleKotlinCallArgument?,
        extensionArgumentReceiverCandidates: List<SimpleKotlinCallArgument>?,
        initialDiagnostics: Collection<KotlinCallDiagnostic>,
        knownSubstitutor: TypeSubstitutor?
    ): SimpleResolutionCandidate {
        val resolvedKtCall = MutableResolvedCallAtom(
            kotlinCall, descriptor, explicitReceiverKind,
            dispatchArgumentReceiver, extensionArgumentReceiver, extensionArgumentReceiverCandidates
        )

        if (ErrorUtils.isError(descriptor)) {
            return SimpleErrorResolutionCandidate(callComponents, resolutionCallbacks, scopeTower, baseSystem, resolvedKtCall)
        }

        val candidate =
            SimpleResolutionCandidate(callComponents, resolutionCallbacks, scopeTower, baseSystem, resolvedKtCall, knownSubstitutor)

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

    override fun createErrorCandidate(): SimpleResolutionCandidate {
        val errorScope = ErrorUtils.createErrorScope(ErrorScopeKind.SCOPE_FOR_ERROR_RESOLUTION_CANDIDATE, kotlinCall.toString())
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
            extensionArgumentReceiverCandidates = null, initialDiagnostics = listOf(), knownSubstitutor = null
        )
    }
}