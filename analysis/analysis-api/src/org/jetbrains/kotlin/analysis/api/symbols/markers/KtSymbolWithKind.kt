/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName


public interface KtSymbolWithKind : KtSymbol {
    public val symbolKind: KtSymbolKind
}

public enum class KtSymbolKind {
    TOP_LEVEL,
    CLASS_MEMBER,
    LOCAL,
    ACCESSOR,
    SAM_CONSTRUCTOR,
}

