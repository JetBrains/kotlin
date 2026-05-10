/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase

internal class SymbolLightValueParameterWithCustomType(
    parameterSymbol: KaValueParameterSymbol,
    containingMethod: SymbolLightMethodBase,
    private val customType: PsiType,
) : SymbolLightValueParameter(parameterSymbol, containingMethod) {
    override fun KaSession.computeType(parameterSymbol: KaParameterSymbol): PsiType = customType
}
