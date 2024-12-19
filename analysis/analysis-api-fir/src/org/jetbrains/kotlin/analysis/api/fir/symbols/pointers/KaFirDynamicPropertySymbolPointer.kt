/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseCachedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.impl.dynamicMembersStorage
import org.jetbrains.kotlin.name.Name

internal class KaFirDynamicPropertySymbolPointer(
    private val name: Name,
    originalSymbol: KaKotlinPropertySymbol?,
) : KaBaseCachedSymbolPointer<KaKotlinPropertySymbol>(originalSymbol) {

    override fun restoreIfNotCached(analysisSession: KaSession): KaKotlinPropertySymbol {
        require(analysisSession is KaFirSession)
        val dynamicScope =
            analysisSession.firSession.dynamicMembersStorage.getDynamicScopeFor(analysisSession.getScopeSessionFor(analysisSession.firSession))
        val functionSymbol = dynamicScope.getProperties(name).single()
        return analysisSession.firSymbolBuilder.callableBuilder.buildCallableSymbol(functionSymbol) as KaKotlinPropertySymbol
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirDynamicPropertySymbolPointer &&
            other.name == name
}
