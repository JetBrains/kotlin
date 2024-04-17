/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer

public abstract class KtScriptSymbol : KtDeclarationSymbol, KtAnnotatedSymbol, KtNamedSymbol, KtSymbolWithMembers {
    context(KtAnalysisSession)
    abstract override fun createPointer(): KtSymbolPointer<KtScriptSymbol>
}