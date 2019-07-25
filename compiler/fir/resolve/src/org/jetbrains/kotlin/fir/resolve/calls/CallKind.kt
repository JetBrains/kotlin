/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

sealed class CallKind {
    abstract fun sequence(): List<ResolutionStage>

    object Function : CallKind() {
        override fun sequence(): List<ResolutionStage> {
            return functionCallResolutionSequence()
        }
    }

    object VariableAccess : CallKind() {
        override fun sequence(): List<ResolutionStage> {
            return qualifiedAccessResolutionSequence()
        }
    }
}

internal fun functionCallResolutionSequence() = listOf(
    CheckVisibility,
    MapArguments,
    CheckExplicitReceiverConsistency,
    CreateFreshTypeVariableSubstitutorStage,
    CheckReceivers.Dispatch,
    CheckReceivers.Extension,
    CheckArguments
)


internal fun qualifiedAccessResolutionSequence() = listOf(
    CheckVisibility,
    DiscriminateSynthetics,
    CheckExplicitReceiverConsistency,
    CreateFreshTypeVariableSubstitutorStage,
    CheckReceivers.Dispatch,
    CheckReceivers.Extension
)