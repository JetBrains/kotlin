/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol

/**
 * This is a temporary bridge to keep [org.jetbrains.kotlin.analysis.api.components.KaResolver.resolveToSymbols] API
 * in a workable state while allowing to move all reference implementations to the Kotlin IntelliJ plugin.
 *
 * This bridge should be dropped once the new resolution API is stabilized and all `KtReference` usages are gone from the public API.
 */
@KaIdeApi
public interface KaResolvableReferenceBridge {
    /**
     * The implementation to be used as the engine
     */
    public fun KaSession.resolveToSymbols(): Collection<KaSymbol>
}
