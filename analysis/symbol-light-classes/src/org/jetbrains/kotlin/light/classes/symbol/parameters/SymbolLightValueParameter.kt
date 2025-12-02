/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.light.classes.symbol.withSymbol

internal open class SymbolLightValueParameter(
    parameterSymbol: KaValueParameterSymbol,
    containingMethod: SymbolLightMethodBase,
) : SymbolLightParameterCommon(parameterSymbol, containingMethod) {
    override fun isDeclaredAsVararg(): Boolean = parameterSymbolPointer.withSymbol(ktModule) {
        (it as KaValueParameterSymbol).isVararg
    }

    // true only if this is "last" `vararg`
    override fun isVarArgs() = isDeclaredAsVararg() && method.parameterList.parameters.lastOrNull() == this
}