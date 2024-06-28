/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.symbols.KaSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.name.ClassId

internal class KaFirSamConstructorSymbolPointer(
    private val ownerClassId: ClassId,
) : KaSymbolPointer<KaSamConstructorSymbol>() {
    @KaImplementationDetail
    override fun restoreSymbol(analysisSession: KaSession): KaSamConstructorSymbol? {
        require(analysisSession is KaFirSession)
        val owner = analysisSession.getClassLikeSymbol(ownerClassId) as? FirRegularClass ?: return null
        val classSymbol = analysisSession.firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(owner.symbol)
        with(analysisSession) {
            return classSymbol.samConstructor
        }
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirSamConstructorSymbolPointer &&
            other.ownerClassId == ownerClassId
}
