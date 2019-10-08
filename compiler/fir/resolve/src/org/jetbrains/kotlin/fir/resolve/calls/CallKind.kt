/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

sealed class CallKind {
    abstract val resolutionSequence: List<ResolutionStage>

    /*
     * Used for resolution of synthetic calls for `when` and `try` expression
     *   that are equal to `fun <K> select(vararg values: K): K`
     */
    object SyntheticSelect : CallKind() {
        override val resolutionSequence: List<ResolutionStage> = listOf(
            MapArguments,
            CreateFreshTypeVariableSubstitutorStage,
            CheckArguments
        )
    }

    object Function : CallKind() {
        override val resolutionSequence: List<ResolutionStage> = listOf(
            CheckVisibility,
            DiscriminateSynthetics,
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
