/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import org.jetbrains.kotlin.analysis.api.symbols.KaCallableImplementationState
import org.jetbrains.kotlin.analysis.api.components.KaCallableImplementationState as KaLegacyCallableImplementationState

internal data class KaCallableExplicitImplementationStateImpl(
    override val isComplete: Boolean,
) : KaCallableImplementationState.Explicit, KaLegacyCallableImplementationState.Explicit {
    override fun toString(): String {
        return "Explicit(isComplete=$isComplete)"
    }
}

internal data class KaCallableInheritedImplementationStateImpl(
    override val isAmbiguous: Boolean,
    override val isOverridable: Boolean,
) : KaCallableImplementationState.Inherited, KaLegacyCallableImplementationState.Inherited {
    override fun toString(): String {
        return "Inherited(isAmbiguous=$isAmbiguous, isOverridable=$isOverridable)"
    }
}

internal object KaCallableMissingImplementationStateImpl : KaCallableImplementationState.Missing, KaLegacyCallableImplementationState.Missing {
    override fun toString(): String = "Missing"
}
