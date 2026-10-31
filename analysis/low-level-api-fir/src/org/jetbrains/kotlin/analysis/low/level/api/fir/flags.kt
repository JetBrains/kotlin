/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.openapi.util.registry.Registry

/**
 * The registry key which enables source-based symbol IDs in LL FIR sessions.
 *
 * @see areSourceBasedSymbolIdsEnabled
 */
@LLFirInternals
const val SOURCE_BASED_SYMBOL_IDS_REGISTRY_KEY: String = "kotlin.analysis.ll.sourceBasedSymbolIds"

/**
 * Whether [LLSymbolIdFactory][org.jetbrains.kotlin.analysis.low.level.api.fir.symbols.id.LLSymbolIdFactory] creates source-based symbol
 * IDs. Otherwise, all symbol IDs are unique, as in the compiler.
 *
 * Source-based symbol IDs are still experimental (see KT-87712), so they are disabled by default. Analysis API tests enable them by setting
 * the [SOURCE_BASED_SYMBOL_IDS_REGISTRY_KEY] system property, which the registry falls back to. (Registry keys declared in plugin XMLs are
 * not loaded in Standalone mode, so they cannot be set via [Registry] directly.)
 *
 * The flag is only checked when a session's symbol ID factory is created, not during symbol ID creation.
 */
@OptIn(LLFirInternals::class)
internal val areSourceBasedSymbolIdsEnabled: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
    Registry.`is`(SOURCE_BASED_SYMBOL_IDS_REGISTRY_KEY, false)
}
