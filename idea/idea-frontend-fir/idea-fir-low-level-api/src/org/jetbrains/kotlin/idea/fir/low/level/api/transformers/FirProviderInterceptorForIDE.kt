/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptor
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId

internal class FirProviderInterceptorForIDE private constructor(
    private val firFile: FirFile,
    private val session: FirSession,
    private val symbolSet: Set<FirClassLikeSymbol<*>>,
    private val classIdToElementMap: Map<ClassId, FirClassLikeDeclaration>
) : FirProviderInterceptor {

    override fun getFirClassifierContainerFileIfAny(symbol: FirClassLikeSymbol<*>): FirFile? =
        if (symbolSet.contains(symbol)) firFile else session.firProvider.getFirClassifierContainerFileIfAny(symbol)

    override fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration? =
        classIdToElementMap[classId] ?: session.firProvider.getFirClassifierByFqName(classId)

    companion object {
        fun createForFirElement(session: FirSession, firFile: FirFile, element: FirElement): FirProviderInterceptor {
            val nodeInfoCollector = object : FirVisitorVoid() {
                val symbolSet = mutableSetOf<FirClassLikeSymbol<*>>()
                val classIdToElementMap = mutableMapOf<ClassId, FirClassLikeDeclaration>()
                override fun visitElement(element: FirElement) {
                    if (element is FirClassLikeDeclaration) {
                        symbolSet.add(element.symbol)
                        classIdToElementMap[element.symbol.classId] = element
                    }
                    element.acceptChildren(this)
                }
            }
            nodeInfoCollector.visitElement(element)

            return FirProviderInterceptorForIDE(firFile, session, nodeInfoCollector.symbolSet, nodeInfoCollector.classIdToElementMap)
        }
    }
}
