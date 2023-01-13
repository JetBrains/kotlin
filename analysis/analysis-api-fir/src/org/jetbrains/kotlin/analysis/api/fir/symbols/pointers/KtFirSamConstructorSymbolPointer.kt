/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.name.ClassId

internal class KtFirSamConstructorSymbolPointer(
    private val ownerClassId: ClassId,
) : KtSymbolPointer<KtSamConstructorSymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtSamConstructorSymbol? {
        require(analysisSession is KtFirAnalysisSession)
        val owner = analysisSession.getClassLikeSymbol(ownerClassId) as? FirRegularClass ?: return null
        val classSymbol = analysisSession.firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(owner.symbol)
        with(analysisSession) {
            return classSymbol.getSamConstructor()
        }
    }

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = this === other ||
            other is KtFirSamConstructorSymbolPointer &&
            other.ownerClassId == ownerClassId
}
