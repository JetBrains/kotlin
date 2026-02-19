/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaContextParameterOwnerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.name.Name

@KaImplementationDetail
class KaBaseContextParameterSymbolPointer(
    private val ownerPointer: KaSymbolPointer<KaContextParameterOwnerSymbol>,
    private val name: Name,
    private val index: Int,
    originalSymbol: KaContextParameterSymbol?,
) : KaBaseCachedSymbolPointer<KaContextParameterSymbol>(originalSymbol) {
    @KaImplementationDetail
    override fun restoreIfNotCached(analysisSession: KaSession): KaContextParameterSymbol? {
        val callableSymbol = with(analysisSession) {
            ownerPointer.restoreSymbol()
        }

        return callableSymbol?.contextParameters?.getOrNull(index)?.takeIf { it.name == name }
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaBaseContextParameterSymbolPointer &&
            other.index == index &&
            other.name == name &&
            other.ownerPointer.pointsToTheSameSymbolAs(ownerPointer)
}
