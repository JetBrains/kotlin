/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.candidate

import org.jetbrains.kotlin.fir.resolve.calls.stages.*

sealed class CallKind(vararg val resolutionSequence: ResolutionStage) {
    object VariableAccess : CallKind(
        CheckHiddenDeclaration,
        CheckVisibility,
        DiscriminateSyntheticAndForbiddenProperties,
        NoTypeArguments,
        InitializeEmptyArgumentMap,
        CreateFreshTypeVariableSubstitutorStage,
        CollectTypeVariableUsagesInfo,
        CheckDispatchReceiver,
        CheckExtensionReceiver,
        CheckStaticExtensionReceiver,
        CheckContextArguments,
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
        DiscriminateSyntheticAndForbiddenProperties,
        MapArguments,
        MapTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CollectTypeVariableUsagesInfo,
        CheckDispatchReceiver,
        CheckExtensionReceiver,
        CheckStaticExtensionReceiver,
        CheckArguments,
        CheckContextArguments,
        CheckDslScopeViolation,
        CheckCallModifiers,
        EagerResolveOfCallableReferences,
        CheckLowPriorityInOverloadResolution,
        ProcessDynamicExtensionAnnotation,
        LowerPriorityIfDynamic,
        ConstraintSystemForks,
        CheckIncompatibleTypeVariableUpperBounds,
        TypeParameterAsCallable,
        TypeVariablesInExplicitReceivers,
        CheckLambdaAgainstTypeVariableContradiction,
    )

    object DelegatingConstructorCall : CallKind(
        CheckHiddenDeclaration,
        CheckVisibility,
        MapArguments,
        MapTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CollectTypeVariableUsagesInfo,
        CheckDispatchReceiver,
        CheckExtensionReceiver,
        CheckStaticExtensionReceiver,
        CheckArguments,
        CheckContextArguments,
        CheckDslScopeViolation,
        EagerResolveOfCallableReferences,
        ConstraintSystemForks,
        CheckIncompatibleTypeVariableUpperBounds,
        CheckLambdaAgainstTypeVariableContradiction,
    )

    object CallableReference : CallKind(
        CheckHiddenDeclaration,
        CheckVisibility,
        DiscriminateSyntheticAndForbiddenProperties,
        NoTypeArguments,
        InitializeEmptyArgumentMap,
        CreateFreshTypeVariableSubstitutorStage,
        CollectTypeVariableUsagesInfo,
        CheckDispatchReceiver,
        CheckExtensionReceiver,
        CheckStaticExtensionReceiver,
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

    final override fun toString(): String {
        return this::class.simpleName ?: super.toString()
    }
}

class ResolutionSequenceBuilder(
    var checkVisibility: Boolean = false,
    var discriminateSynthetics: Boolean = false,
    var checkDispatchReceiver: Boolean = false,
    var checkExtensionReceiver: Boolean = false,
    var checkArguments: Boolean = false,
    var checkLowPriorityInOverloadResolution: Boolean = false,
    var mapTypeArguments: Boolean = false,
    var resolveCallableReferenceArguments: Boolean = false,
    var checkCallableReferenceExpectedType: Boolean = false,
    val checkContextParameters: Boolean = false,
) {
    fun build(): CallKind {
        val stages = mutableListOf<ResolutionStage>().apply {
            if (checkVisibility) add(CheckVisibility)
            if (discriminateSynthetics) add(DiscriminateSyntheticAndForbiddenProperties)
            if (checkArguments) add(MapArguments) else add(InitializeEmptyArgumentMap)
            if (mapTypeArguments) add(MapTypeArguments) else add(NoTypeArguments)
            if (checkArguments || checkDispatchReceiver || checkExtensionReceiver) add(CreateFreshTypeVariableSubstitutorStage)
            if (checkDispatchReceiver) add(CheckDispatchReceiver)
            if (checkExtensionReceiver) {
                add(CheckExtensionReceiver)
                add(CheckStaticExtensionReceiver)
            }
            if (checkArguments) add(CheckArguments)
            if (checkContextParameters) add(CheckContextArguments)
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
