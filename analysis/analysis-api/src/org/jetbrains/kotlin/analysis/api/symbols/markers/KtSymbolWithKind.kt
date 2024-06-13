/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol


public interface KaSymbolWithKind : KaSymbol {
    public val symbolKind: KaSymbolKind
}

public typealias KtSymbolWithKind = KaSymbolWithKind

public enum class KaSymbolKind {
    TOP_LEVEL,
    CLASS_MEMBER,
    LOCAL,
    ACCESSOR,
    SAM_CONSTRUCTOR,
}

public typealias KtSymbolKind = KaSymbolKind