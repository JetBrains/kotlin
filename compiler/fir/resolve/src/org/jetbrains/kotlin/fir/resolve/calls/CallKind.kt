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
        CreateFreshTypeVariableSubstitutorStage,
        CheckReceivers.Dispatch,
        CheckReceivers.Extension
    ),
    SyntheticSelect(
        MapArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CheckArguments
    ),
    Function(
        CheckVisibility,
        DiscriminateSynthetics,
        MapArguments,
        CheckExplicitReceiverConsistency,
        CreateFreshTypeVariableSubstitutorStage,
        CheckReceivers.Dispatch,
        CheckReceivers.Extension,
        CheckArguments,
        EagerResolveOfCallableReferences
    ),
    CallableReference(
        CheckVisibility,
        DiscriminateSynthetics,
        CreateFreshTypeVariableSubstitutorStage,
        CheckCallableReferenceExpectedType
    ),
    SyntheticIdForCallableReferencesResolution(
        MapArguments,
        CreateFreshTypeVariableSubstitutorStage,
        CheckArguments,
        EagerResolveOfCallableReferences
    );

    val resolutionSequence: List<ResolutionStage> = resolutionSequence.toList()
}
