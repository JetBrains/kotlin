/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

sealed class CallKind(vararg resolutionSequence: ResolutionStage) {
    object VariableAccess : CallKind(
        CheckVisibility,
        DiscriminateSynthetics,
        CheckExplicitReceiverConsistency,
        NoTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CheckReceivers.Dispatch,
        CheckReceivers.Extension,
        CheckLowPriorityInOverloadResolution,
        PostponedVariablesInitializerResolutionStage
    )

    object SyntheticSelect : CallKind(
        MapArguments,
        NoTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CheckArguments,
        EagerResolveOfCallableReferences
    )

    object Function : CallKind(
        CheckVisibility,
        DiscriminateSynthetics,
        MapArguments,
        CheckExplicitReceiverConsistency,
        MapTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CheckReceivers.Dispatch,
        CheckReceivers.Extension,
        CheckArguments,
        EagerResolveOfCallableReferences,
        CheckLowPriorityInOverloadResolution,
        PostponedVariablesInitializerResolutionStage
    )

    object DelegatingConstructorCall : CallKind(
        CheckVisibility,
        MapArguments,
        CheckExplicitReceiverConsistency,
        MapTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CheckReceivers.Dispatch,
        CheckReceivers.Extension,
        CheckArguments,
        EagerResolveOfCallableReferences
    )

    object CallableReference : CallKind(
        CheckVisibility,
        DiscriminateSynthetics,
        NoTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CheckReceivers.Dispatch,
        CheckReceivers.Extension,
        CheckCallableReferenceExpectedType,
        CheckLowPriorityInOverloadResolution
    )

    object SyntheticIdForCallableReferencesResolution : CallKind(
        MapArguments,
        NoTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CheckArguments,
        EagerResolveOfCallableReferences
    )

    internal class CustomForIde(vararg resolutionSequence: ResolutionStage) : CallKind(*resolutionSequence)

    val resolutionSequence: List<ResolutionStage> = resolutionSequence.toList()

    final override fun toString(): String {
        return this::class.simpleName ?: super.toString()
    }
}

class ResolutionSequenceBuilder(
    var checkVisibility: Boolean = false,
    var discriminateSynthetics: Boolean = false,
    var checkExplicitReceiverConsistency: Boolean = false,
    var checkDispatchReceiver: Boolean = false,
    var checkExtensionReceiver: Boolean = false,
    var checkArguments: Boolean = false,
    var checkLowPriorityInOverloadResolution: Boolean = false,
    var initializePostponedVariables: Boolean = false,
    var mapTypeArguments: Boolean = false,
    var resolveCallableReferenceArguments: Boolean = false,
    var checkCallableReferenceExpectedType: Boolean = false,
) {
    fun build(): CallKind {
        val stages = mutableListOf<ResolutionStage>().apply {
            if (checkVisibility) add(CheckVisibility)
            if (discriminateSynthetics) add(DiscriminateSynthetics)
            if (checkArguments) add(MapArguments)
            if (checkExplicitReceiverConsistency) add(CheckExplicitReceiverConsistency)
            if (mapTypeArguments) add(MapTypeArguments) else add(NoTypeArguments)
            if (checkArguments || checkDispatchReceiver || checkExtensionReceiver) add(CreateFreshTypeVariableSubstitutorStage)
            if (checkDispatchReceiver) add(CheckReceivers.Dispatch)
            if (checkExtensionReceiver) add(CheckReceivers.Extension)
            if (checkArguments) add(CheckArguments)
            if (resolveCallableReferenceArguments) add(EagerResolveOfCallableReferences)
            if (checkLowPriorityInOverloadResolution) add(CheckLowPriorityInOverloadResolution)
            if (initializePostponedVariables) add(PostponedVariablesInitializerResolutionStage)
            if (checkCallableReferenceExpectedType) add(CheckCallableReferenceExpectedType)
        }.toTypedArray()
        return CallKind.CustomForIde(*stages)
    }
}

fun buildCallKindWithCustomResolutionSequence(init: ResolutionSequenceBuilder.() -> Unit): CallKind {
    return ResolutionSequenceBuilder().apply(init).build()
}
