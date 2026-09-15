/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.containingDeclaration
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.withNullability
import org.jetbrains.kotlin.light.classes.symbol.classes.hasBoxedParameterForSpecialCaseOfRemove
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

    /**
     * Whether the JVM backend boxes the parameter regardless of its declared type, see [hasBoxedParameterForSpecialCaseOfRemove].
     */
    context(_: KaSession)
    protected open fun isBoxedForSpecialCaseOfRemove(parameterSymbol: KaParameterSymbol): Boolean =
        (parameterSymbol.containingDeclaration as? KaNamedFunctionSymbol)?.hasBoxedParameterForSpecialCaseOfRemove() == true

    context(_: KaSession)
    override fun kotlinType(parameterSymbol: KaParameterSymbol): KaType {
        val type = super.kotlinType(parameterSymbol)
        // The JVM backend boxes the parameter by making its type nullable, so the light parameter is nullable as well
        return if (isBoxedForSpecialCaseOfRemove(parameterSymbol)) type.withNullability(true) else type
    }
}
