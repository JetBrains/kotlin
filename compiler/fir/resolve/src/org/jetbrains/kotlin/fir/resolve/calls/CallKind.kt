/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

sealed class CallKind(vararg resolutionSequence: ResolutionStage) {
    object VariableAccess : CallKind(
        CheckHiddenDeclaration,
        CheckVisibility,
        DiscriminateSynthetics,
        CheckExplicitReceiverConsistency,
        NoTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CollectTypeVariableUsagesInfo,
        CheckDispatchReceiver,
        CheckExtensionReceiver,
        CheckContextReceivers,
        CheckDslScopeViolation,
        CheckLowPriorityInOverloadResolution,
        ProcessDynamicExtensionAnnotation,
        LowerPriorityIfDynamic,
        ConstraintSystemForks,
        CheckIncompatibleTypeVariableUpperBounds,
        TypeParameterAsCallable,
        TypeVariablesInExplicitReceivers,
    )

    object SyntheticSelect : CallKind(
        MapArguments,
        NoTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CollectTypeVariableUsagesInfo,
        CheckArguments,
        EagerResolveOfCallableReferences,
        ConstraintSystemForks,
        CheckIncompatibleTypeVariableUpperBounds,
    )

    object Function : CallKind(
        CheckHiddenDeclaration,
        CheckVisibility,
        DiscriminateSynthetics,
        MapArguments,
        CheckExplicitReceiverConsistency,
        MapTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CollectTypeVariableUsagesInfo,
        CheckDispatchReceiver,
        CheckExtensionReceiver,
        CheckContextReceivers,
        CheckDslScopeViolation,
        CheckArguments,
        CheckCallModifiers,
        EagerResolveOfCallableReferences,
        CheckLowPriorityInOverloadResolution,
        ProcessDynamicExtensionAnnotation,
        LowerPriorityIfDynamic,
        ConstraintSystemForks,
        CheckIncompatibleTypeVariableUpperBounds,
        TypeParameterAsCallable,
        TypeVariablesInExplicitReceivers,
    )

    object DelegatingConstructorCall : CallKind(
        CheckHiddenDeclaration,
        CheckVisibility,
        MapArguments,
        CheckExplicitReceiverConsistency,
        MapTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CollectTypeVariableUsagesInfo,
        CheckDispatchReceiver,
        CheckExtensionReceiver,
        CheckContextReceivers,
        CheckDslScopeViolation,
        CheckArguments,
        EagerResolveOfCallableReferences,
        ConstraintSystemForks,
        CheckIncompatibleTypeVariableUpperBounds,
    )

    object CallableReference : CallKind(
        CheckHiddenDeclaration,
        CheckVisibility,
        DiscriminateSynthetics,
        NoTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CollectTypeVariableUsagesInfo,
        CheckDispatchReceiver,
        CheckExtensionReceiver,
        CheckDslScopeViolation,
        CheckCallableReferenceExpectedType,
        CheckLowPriorityInOverloadResolution,
        CheckIncompatibleTypeVariableUpperBounds,
        ProcessDynamicExtensionAnnotation,
        LowerPriorityIfDynamic,
        TypeVariablesInExplicitReceivers,
    )

    object SyntheticIdForCallableReferencesResolution : CallKind(
        MapArguments,
        MapTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CollectTypeVariableUsagesInfo,
        CheckArguments,
        EagerResolveOfCallableReferences,
        ConstraintSystemForks,
        CheckIncompatibleTypeVariableUpperBounds,
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
    var mapTypeArguments: Boolean = false,
    var resolveCallableReferenceArguments: Boolean = false,
    var checkCallableReferenceExpectedType: Boolean = false,
    val checkContextReceivers: Boolean = false,
) {
    fun build(): CallKind {
        val stages = mutableListOf<ResolutionStage>().apply {
            if (checkVisibility) add(CheckVisibility)
            if (discriminateSynthetics) add(DiscriminateSynthetics)
            if (checkArguments) add(MapArguments)
            if (checkExplicitReceiverConsistency) add(CheckExplicitReceiverConsistency)
            if (mapTypeArguments) add(MapTypeArguments) else add(NoTypeArguments)
            if (checkArguments || checkDispatchReceiver || checkExtensionReceiver) add(CreateFreshTypeVariableSubstitutorStage)
            if (checkDispatchReceiver) add(CheckDispatchReceiver)
            if (checkExtensionReceiver) add(CheckExtensionReceiver)
            if (checkArguments) add(CheckArguments)
            if (checkContextReceivers) add(CheckContextReceivers)
            if (resolveCallableReferenceArguments) add(EagerResolveOfCallableReferences)
            if (checkLowPriorityInOverloadResolution) add(CheckLowPriorityInOverloadResolution)
            if (checkCallableReferenceExpectedType) add(CheckCallableReferenceExpectedType)
        }.toTypedArray()
        return CallKind.CustomForIde(*stages)
    }
}

fun buildCallKindWithCustomResolutionSequence(init: ResolutionSequenceBuilder.() -> Unit): CallKind {
    return ResolutionSequenceBuilder().apply(init).build()
}
