/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.restoreSymbol

internal sealed class KaFirPropertyAccessorSymbolPointer<T : KaPropertyAccessorSymbol>(
    private val propertySymbolPointer: KaSymbolPointer<KaPropertySymbol>,
    originalSymbol: T?,
) : KaFirCachedSymbolPointer<T>(originalSymbol) {
    context(session: KaSession)
    protected fun restorePropertySymbol(): KaPropertySymbol? = propertySymbolPointer.restoreSymbol()

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean {
        return this === other ||
                other is KaFirPropertyAccessorSymbolPointer<*> &&
                other.javaClass == javaClass &&
                other.propertySymbolPointer.pointsToTheSameSymbolAs(propertySymbolPointer)
    }
}

internal class KaFirPropertyGetterSymbolPointer(
    propertySymbolPointer: KaSymbolPointer<KaPropertySymbol>,
    originalSymbol: KaPropertyGetterSymbol?,
) : KaFirPropertyAccessorSymbolPointer<KaPropertyGetterSymbol>(propertySymbolPointer, originalSymbol) {
    @KaImplementationDetail
    context(session: KaSession)
    override fun restoreIfNotCached(): KaPropertyGetterSymbol? {
        return restorePropertySymbol()?.getter
    }
}

internal class KaFirPropertySetterSymbolPointer(
    propertySymbolPointer: KaSymbolPointer<KaPropertySymbol>,
    originalSymbol: KaPropertySetterSymbol?,
) : KaFirPropertyAccessorSymbolPointer<KaPropertySetterSymbol>(propertySymbolPointer, originalSymbol) {
    @KaImplementationDetail
    context(session: KaSession)
    override fun restoreIfNotCached(): KaPropertySetterSymbol? {
        return restorePropertySymbol()?.setter
    }
}
