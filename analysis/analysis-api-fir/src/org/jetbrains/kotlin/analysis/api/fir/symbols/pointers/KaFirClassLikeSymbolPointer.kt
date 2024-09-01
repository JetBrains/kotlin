/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.name.ClassId
import kotlin.reflect.KClass

internal class KaFirClassLikeSymbolPointer<T : KaClassLikeSymbol>(
    private val classId: ClassId,
    private val expectedClass: KClass<T>,
) : KaSymbolPointer<T>() {
    @KaImplementationDetail
    override fun restoreSymbol(analysisSession: KaSession): T? {
        require(analysisSession is KaFirSession)
        val classLikeSymbol = analysisSession.firSymbolBuilder.classifierBuilder.buildClassLikeSymbolByClassId(classId) ?: return null
        if (!expectedClass.isInstance(classLikeSymbol)) return null

        @Suppress("UNCHECKED_CAST")
        return classLikeSymbol as T
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = other === this ||
            other is KaFirClassLikeSymbolPointer &&
            other.classId == classId &&
            other.expectedClass == expectedClass
}
