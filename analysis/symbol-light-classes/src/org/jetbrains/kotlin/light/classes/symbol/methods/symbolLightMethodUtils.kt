/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase

internal fun KaSession.isSuppressedFinalModifier(string: String, containingClass: SymbolLightClassBase, symbol: KaCallableSymbol): Boolean {
    return string == PsiModifier.FINAL && (containingClass.isEnum && symbol.origin == KaSymbolOrigin.SOURCE_MEMBER_GENERATED || containingClass.isInterface)
}
