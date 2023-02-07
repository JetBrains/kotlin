/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase

context(KtAnalysisSession)
internal fun String.isSuppressedFinalModifier(containingClass: SymbolLightClassBase, symbol: KtCallableSymbol): Boolean {
    return this == PsiModifier.FINAL && (containingClass.isEnum && symbol.origin == KtSymbolOrigin.SOURCE_MEMBER_GENERATED || containingClass.isInterface)
}
