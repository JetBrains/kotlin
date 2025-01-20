/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.base

import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntryInitializerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

/**
 * This class is required to provide correct type for [createPointer] to avoid intersection override,
 * so it can be returned from both [org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DescEnumEntrySymbol]
 * and [org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.KaFe10PsiEnumEntrySymbol]
 */
internal abstract class KaFe10EnumEntrySymbol : KaEnumEntrySymbol(), KaEnumEntryInitializerSymbol {
    abstract override fun createPointer(): KaSymbolPointer<KaFe10EnumEntrySymbol>
}
