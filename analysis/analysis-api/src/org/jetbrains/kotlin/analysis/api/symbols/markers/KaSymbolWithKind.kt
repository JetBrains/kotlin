/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation

/**
 * @see org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
 */
@Deprecated("Use `location` directly from `KaSymbol`")
public interface KaSymbolWithKind : KaSymbol {
    @Deprecated("Use `location` instead", ReplaceWith("location"))
    @Suppress("DEPRECATION")
    public val symbolKind: KaSymbolKind
        get() = when (location) {
            KaSymbolLocation.TOP_LEVEL -> KaSymbolKind.TOP_LEVEL
            KaSymbolLocation.CLASS -> KaSymbolKind.CLASS_MEMBER
            KaSymbolLocation.PROPERTY -> KaSymbolKind.ACCESSOR
            KaSymbolLocation.LOCAL -> KaSymbolKind.LOCAL
        }
}

@Deprecated("Use `location` from `KaSymbol`")
public typealias KtSymbolWithKind = @Suppress("DEPRECATION") KaSymbolWithKind

@Deprecated("Use `KaSymbolLocation` instead", ReplaceWith("KaSymbolLocation"))
public enum class KaSymbolKind {
    TOP_LEVEL,
    CLASS_MEMBER,
    LOCAL,
    ACCESSOR,
    SAM_CONSTRUCTOR,
}

@Deprecated("Use `KaSymbolLocation` instead", ReplaceWith("KaSymbolLocation"))
public typealias KtSymbolKind = @Suppress("DEPRECATION") KaSymbolKind