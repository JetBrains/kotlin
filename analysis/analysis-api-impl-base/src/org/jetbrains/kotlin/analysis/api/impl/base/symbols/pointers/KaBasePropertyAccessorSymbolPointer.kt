/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

@KaImplementationDetail
sealed class KaBasePropertyAccessorSymbolPointer<T : KaPropertyAccessorSymbol>(
    private val propertySymbolPointer: KaSymbolPointer<KaPropertySymbol>,
    originalSymbol: T?,
) : KaBaseCachedSymbolPointer<T>(originalSymbol) {
    protected fun restorePropertySymbol(analysisSession: KaSession): KaPropertySymbol? = with(analysisSession) {
        return propertySymbolPointer.restoreSymbol()
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean {
        return this === other ||
                other is KaBasePropertyAccessorSymbolPointer<*> &&
                other.javaClass == javaClass &&
                other.propertySymbolPointer.pointsToTheSameSymbolAs(propertySymbolPointer)
    }
}

@KaImplementationDetail
class KaBasePropertyGetterSymbolPointer(
    propertySymbolPointer: KaSymbolPointer<KaPropertySymbol>,
    originalSymbol: KaPropertyGetterSymbol?,
) : KaBasePropertyAccessorSymbolPointer<KaPropertyGetterSymbol>(propertySymbolPointer, originalSymbol) {
    @KaImplementationDetail
    override fun restoreIfNotCached(analysisSession: KaSession): KaPropertyGetterSymbol? {
        return restorePropertySymbol(analysisSession)?.getter
    }
}

@KaImplementationDetail
class KaBasePropertySetterSymbolPointer(
    propertySymbolPointer: KaSymbolPointer<KaPropertySymbol>,
    originalSymbol: KaPropertySetterSymbol?,
) : KaBasePropertyAccessorSymbolPointer<KaPropertySetterSymbol>(propertySymbolPointer, originalSymbol) {
    @KaImplementationDetail
    override fun restoreIfNotCached(analysisSession: KaSession): KaPropertySetterSymbol? {
        return restorePropertySymbol(analysisSession)?.setter
    }
}