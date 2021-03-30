/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers

import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.ClassId

class KtFirClassOrObjectInLibrarySymbolPointer(private val classId: ClassId) : KtSymbolPointer<KtNamedClassOrObjectSymbol>() {
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtNamedClassOrObjectSymbol? {
        require(analysisSession is KtFirAnalysisSession)
        return analysisSession.firSymbolBuilder.classifierBuilder.buildClassLikeSymbolByClassId(classId) as? KtNamedClassOrObjectSymbol
    }
}