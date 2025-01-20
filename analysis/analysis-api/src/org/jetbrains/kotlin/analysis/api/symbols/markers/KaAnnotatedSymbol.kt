/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

/**
 * A [KaSymbol] representing a declaration which may have annotations applied to it.
 *
 * @see KaAnnotated
 */
public interface KaAnnotatedSymbol : KaSymbol, KaAnnotated {
    override fun createPointer(): KaSymbolPointer<KaAnnotatedSymbol>
}
