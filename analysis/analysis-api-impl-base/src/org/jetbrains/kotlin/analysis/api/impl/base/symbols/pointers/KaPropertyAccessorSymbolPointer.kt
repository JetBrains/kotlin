/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

@KaAnalysisApiInternals
sealed class KaPropertyAccessorSymbolPointer<T : KaPropertyAccessorSymbol>(
    private val propertySymbolPointer: KaSymbolPointer<KaPropertySymbol>
) : KaSymbolPointer<T>() {
    protected fun restorePropertySymbol(analysisSession: KaSession): KaPropertySymbol? = with(analysisSession) {
        return propertySymbolPointer.restoreSymbol()
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean {
        return this === other ||
                other is KaPropertyAccessorSymbolPointer<*> &&
                other.javaClass == javaClass &&
                other.propertySymbolPointer.pointsToTheSameSymbolAs(propertySymbolPointer)
    }
}

@KaAnalysisApiInternals
class KaPropertyGetterSymbolPointer(
    propertySymbolPointer: KaSymbolPointer<KaPropertySymbol>,
) : KaPropertyAccessorSymbolPointer<KaPropertyGetterSymbol>(propertySymbolPointer) {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KaSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KaSession): KaPropertyGetterSymbol? {
        return restorePropertySymbol(analysisSession)?.getter
    }
}

@KaAnalysisApiInternals
class KaPropertySetterSymbolPointer(
    propertySymbolPointer: KaSymbolPointer<KaPropertySymbol>,
) : KaPropertyAccessorSymbolPointer<KaPropertySetterSymbol>(propertySymbolPointer) {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KaSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KaSession): KaPropertySetterSymbol? {
        return restorePropertySymbol(analysisSession)?.setter
    }
}