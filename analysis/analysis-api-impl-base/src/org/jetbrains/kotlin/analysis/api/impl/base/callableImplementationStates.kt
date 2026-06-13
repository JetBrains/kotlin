/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaExperimentalApi::class)

package org.jetbrains.kotlin.analysis.api.impl.base

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.KaCallableImplementationState

@KaImplementationDetail
data class KaCallableExplicitImplementationStateImpl(
    override val isComplete: Boolean,
) : KaCallableImplementationState.Explicit {
    override fun toString(): String {
        return "Explicit(isComplete=$isComplete)"
    }
}

@KaImplementationDetail
data class KaCallableInheritedImplementationStateImpl(
    override val isAmbiguous: Boolean,
    override val isOverridable: Boolean,
) : KaCallableImplementationState.Inherited {
    override fun toString(): String {
        return "Inherited(isAmbiguous=$isAmbiguous, isOverridable=$isOverridable)"
    }
}

@KaImplementationDetail
object KaCallableMissingImplementationStateImpl : KaCallableImplementationState.Missing {
    override fun toString(): String = "Missing"
}
