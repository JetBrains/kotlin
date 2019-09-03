/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

sealed class CallKind {
    abstract val resolutionSequence: List<ResolutionStage>

    object Function : CallKind() {
        override val resolutionSequence: List<ResolutionStage> = listOf(
            CheckVisibility,
            MapArguments,
            CheckExplicitReceiverConsistency,
            CreateFreshTypeVariableSubstitutorStage,
            CheckReceivers.Dispatch,
            CheckReceivers.Extension,
            CheckArguments
        )
    }

    object VariableAccess : CallKind() {
        override val resolutionSequence: List<ResolutionStage> = listOf(
            CheckVisibility,
            DiscriminateSynthetics,
            CheckExplicitReceiverConsistency,
            CreateFreshTypeVariableSubstitutorStage,
            CheckReceivers.Dispatch,
            CheckReceivers.Extension
        )
    }
}