/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer

@KtAnalysisApiInternals
class KtPropertyAccessorSymbolPointer(
    private val propertySymbolPointer: KtSymbolPointer<KtPropertySymbol>,
    private val isGetter: Boolean,
) : KtSymbolPointer<KtPropertyAccessorSymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtPropertyAccessorSymbol? {
        val propertySymbol = with(analysisSession) {
            propertySymbolPointer.restoreSymbol()
        } ?: return null

        return if (isGetter) propertySymbol.getter else propertySymbol.setter
    }

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = this === other ||
            other is KtPropertyAccessorSymbolPointer &&
            other.isGetter == isGetter &&
            other.propertySymbolPointer.pointsToTheSameSymbolAs(propertySymbolPointer)
}
