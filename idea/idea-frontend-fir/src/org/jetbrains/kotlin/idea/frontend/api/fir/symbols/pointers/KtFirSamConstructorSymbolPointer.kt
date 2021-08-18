/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers

import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSamConstructorSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.ClassId

internal class KtFirSamConstructorSymbolPointer(
    private val ownerClassId: ClassId,
) : KtSymbolPointer<KtSamConstructorSymbol>() {
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtSamConstructorSymbol? {
        require(analysisSession is KtFirAnalysisSession)
        val owner = analysisSession.getClassLikeSymbol(ownerClassId) as? FirRegularClass ?: return null
        val classSymbol = analysisSession.firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(owner)
        with(analysisSession) {
            return classSymbol.getSamConstructor()
        }
    }
}
