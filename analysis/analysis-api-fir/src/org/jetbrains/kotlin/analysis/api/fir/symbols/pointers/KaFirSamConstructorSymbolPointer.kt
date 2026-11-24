/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.restoreSymbol
import org.jetbrains.kotlin.analysis.api.symbols.samConstructor

internal class KaFirSamConstructorSymbolPointer(
    private val ownerPointer: KaSymbolPointer<KaClassLikeSymbol>,
    originalSymbol: KaSamConstructorSymbol?,
) : KaFirCachedSymbolPointer<KaSamConstructorSymbol>(originalSymbol) {
    @KaImplementationDetail
    context(session: KaSession)
    override fun restoreIfNotCached(): KaSamConstructorSymbol? = ownerPointer.restoreSymbol()?.samConstructor

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirSamConstructorSymbolPointer &&
            ownerPointer.pointsToTheSameSymbolAs(other.ownerPointer)
}
