/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

internal class KaFirSamConstructorSymbolPointer(
    private val samInterfacePointer: KaSymbolPointer<KaClassLikeSymbol>,
    originalSymbol: KaSamConstructorSymbol?,
) : KaFirCachedSymbolPointer<KaSamConstructorSymbol>(originalSymbol) {
    @KaImplementationDetail
    override fun restoreIfNotCached(analysisSession: KaSession): KaSamConstructorSymbol? = with(analysisSession) {
        samInterfacePointer.restoreSymbol()?.samConstructor
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirSamConstructorSymbolPointer &&
            samInterfacePointer.pointsToTheSameSymbolAs(other.samInterfacePointer)
}
