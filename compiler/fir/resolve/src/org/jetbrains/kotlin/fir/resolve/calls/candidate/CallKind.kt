/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.candidate

import org.jetbrains.kotlin.fir.resolve.calls.stages.*

sealed class CallKind(
    vararg val resolutionSequence: ResolutionStage,
    /**
     * Stages that will be run after candidate is chosen.
     * Currently only used for collection literals.
     */
    additionalStages: Array<ResolutionStage> = emptyArray(),
) {
    val resolutionSequenceWithAdditionalStages: Array<out ResolutionStage> = arrayOf(*resolutionSequence, *additionalStages)

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
        CheckContextArguments,
        CheckShadowedImplicits,
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
        CheckArguments,
        CheckContextArguments,
        CheckShadowedImplicits,
        CheckCallModifiers,
        EagerResolveOfCallableReferences,
        EagerResolveOfCollectionLiteral,
        CheckLowPriorityInOverloadResolution,
        ProcessDynamicExtensionAnnotation,
        LowerPriorityIfDynamic,
        ConstraintSystemForks,
        CheckIncompatibleTypeVariableUpperBounds,
        TypeParameterAsCallable,
        TypeVariablesInExplicitReceivers,
        CheckLambdaAgainstTypeVariableContradiction,
    )

    /**
     * For collection literal, we only need stages that either:
     * 1. are part of candidate constraint system construction, or
     * 2. in green code, ensure that we choose the correct one among operator `of`s ([MapArguments], [CheckCallModifiers], ...)
     *
     * Stages like [EagerResolveOfCallableReferences] do not help to choose the candidate for **collection literal** call,
     * but they need to be run **after** the candidate is chosen to help with overload resolution of outer
     * call. This is done in scope of [EagerResolveOfCollectionLiteral] for outer call.
     */
    object CollectionLiteral : CallKind(
        CheckHiddenDeclaration,
        MapArguments,
        MapTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CollectTypeVariableUsagesInfo,
        CheckCallModifiers,
        CheckLowPriorityInOverloadResolution,
        additionalStages = arrayOf(
            CheckVisibility,
            CheckArguments,
            CheckDispatchReceiver,
            CheckExtensionReceiver,
            CheckContextArguments,
            CheckShadowedImplicits,
            EagerResolveOfCollectionLiteral,
            EagerResolveOfCallableReferences,
            CheckLambdaAgainstTypeVariableContradiction,
            CheckIncompatibleTypeVariableUpperBounds,
        )
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
        CheckArguments,
        CheckContextArguments,
        CheckShadowedImplicits,
        EagerResolveOfCallableReferences,
        EagerResolveOfCollectionLiteral,
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
        CheckShadowedImplicits,
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
            if (checkExtensionReceiver) add(CheckExtensionReceiver)
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
