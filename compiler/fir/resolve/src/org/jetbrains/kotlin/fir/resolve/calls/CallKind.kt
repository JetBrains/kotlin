/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

enum class CallKind(vararg resolutionSequence: ResolutionStage) {
    VariableAccess(
        CheckVisibility,
        DiscriminateSynthetics,
        CheckExplicitReceiverConsistency,
        NoTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CheckReceivers.Dispatch,
        CheckReceivers.Extension,
        CheckLowPriorityInOverloadResolution
    ),
    SyntheticSelect(
        MapArguments,
        NoTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CheckArguments,
        EagerResolveOfCallableReferences
    ),
    Function(
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
        CheckLowPriorityInOverloadResolution
    ),
    DelegatingConstructorCall(
        CheckVisibility,
        MapArguments,
        CheckExplicitReceiverConsistency,
        MapTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CheckReceivers.Dispatch,
        CheckReceivers.Extension,
        CheckArguments,
        EagerResolveOfCallableReferences
    ),
    CallableReference(
        CheckVisibility,
        DiscriminateSynthetics,
        NoTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CheckReceivers.Dispatch,
        CheckReceivers.Extension,
        CheckCallableReferenceExpectedType
    ),
    SyntheticIdForCallableReferencesResolution(
        MapArguments,
        NoTypeArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CheckArguments,
        EagerResolveOfCallableReferences
    );

    val resolutionSequence: List<ResolutionStage> = resolutionSequence.toList()
}
