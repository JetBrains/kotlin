/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import org.jetbrains.kotlin.analysis.api.symbols.KaCallableImplementationState
import org.jetbrains.kotlin.util.ImplementationStatus
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

internal fun ImplementationStatus.toKaImplementationState(): KaCallableImplementationState = when (this) {
    ImplementationStatus.NOT_IMPLEMENTED -> KaCallableMissingImplementationStateImpl
    ImplementationStatus.VAR_IMPLEMENTED_BY_VAL -> KaCallableExplicitImplementationStateImpl(isComplete = false)
    ImplementationStatus.AMBIGUOUSLY_INHERITED -> KaCallableInheritedImplementationStateImpl(isAmbiguous = true, isOverridable = true)
    ImplementationStatus.INHERITED_OR_SYNTHESIZED -> KaCallableInheritedImplementationStateImpl(isAmbiguous = false, isOverridable = true)
    ImplementationStatus.ALREADY_IMPLEMENTED -> KaCallableExplicitImplementationStateImpl(isComplete = true)
    ImplementationStatus.CANNOT_BE_IMPLEMENTED -> KaCallableInheritedImplementationStateImpl(isAmbiguous = false, isOverridable = false)
}
