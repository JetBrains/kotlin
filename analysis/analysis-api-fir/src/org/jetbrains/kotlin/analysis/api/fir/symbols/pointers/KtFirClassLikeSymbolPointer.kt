/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.ClassId
import kotlin.reflect.KClass

internal class KtFirClassLikeSymbolPointer<T : KtClassLikeSymbol>(
    private val classId: ClassId,
    private val expectedClass: KClass<T>,
) : KtSymbolPointer<T>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): T? {
        require(analysisSession is KtFirAnalysisSession)
        val classLikeSymbol = analysisSession.firSymbolBuilder.classifierBuilder.buildClassLikeSymbolByClassId(classId) ?: return null
        if (!expectedClass.isInstance(classLikeSymbol)) return null

        @Suppress("UNCHECKED_CAST")
        return classLikeSymbol as T
    }
}
