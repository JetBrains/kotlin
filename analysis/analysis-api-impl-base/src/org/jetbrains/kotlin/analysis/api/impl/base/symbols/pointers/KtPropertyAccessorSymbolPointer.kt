/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer

@KtAnalysisApiInternals
sealed class KtPropertyAccessorSymbolPointer<T : KtPropertyAccessorSymbol>(
    private val propertySymbolPointer: KtSymbolPointer<KtPropertySymbol>
) : KtSymbolPointer<T>() {
    protected fun restorePropertySymbol(analysisSession: KtAnalysisSession): KtPropertySymbol? = with(analysisSession) {
        return propertySymbolPointer.restoreSymbol()
    }

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean {
        return this === other ||
                other is KtPropertyAccessorSymbolPointer<*> &&
                other.javaClass == javaClass &&
                other.propertySymbolPointer.pointsToTheSameSymbolAs(propertySymbolPointer)
    }
}

@KtAnalysisApiInternals
class KtPropertyGetterSymbolPointer(
    propertySymbolPointer: KtSymbolPointer<KtPropertySymbol>,
) : KtPropertyAccessorSymbolPointer<KtPropertyGetterSymbol>(propertySymbolPointer) {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtPropertyGetterSymbol? {
        return restorePropertySymbol(analysisSession)?.getter
    }
}

@KtAnalysisApiInternals
class KtPropertySetterSymbolPointer(
    propertySymbolPointer: KtSymbolPointer<KtPropertySymbol>,
) : KtPropertyAccessorSymbolPointer<KtPropertySetterSymbol>(propertySymbolPointer) {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtPropertySetterSymbol? {
        return restorePropertySymbol(analysisSession)?.setter
    }
}