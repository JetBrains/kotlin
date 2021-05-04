/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.trasformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirApplySupertypesTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirSupertypeResolverVisitor
import org.jetbrains.kotlin.fir.resolve.transformers.SupertypeComputationSession
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignation
import org.jetbrains.kotlin.idea.fir.low.level.api.trasformers.FirLazyTransformerForIDE.Companion.ensurePhase
import org.jetbrains.kotlin.name.ClassId

internal class FirDesignatedSupertypeResolverTransformerForIDE(
    private val designation: FirDeclarationDesignation,
    private val firFile: FirFile,
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    ) : FirLazyTransformerForIDE {

    private val supertypeComputationSession = SupertypeComputationSession()

    private inner class SupertypeResolver(
        private val symbolSet: Set<FirClassLikeSymbol<*>>,
        private val classIdToElementMap: Map<ClassId, FirClassLikeDeclaration<*>>
    ) : FirSupertypeResolverVisitor(session, supertypeComputationSession, scopeSession) {
        override fun getFirClassifierContainerFile(symbol: FirClassLikeSymbol<*>): FirFile =
            if (symbolSet.contains(symbol)) firFile else session.firProvider.getFirClassifierContainerFile(symbol)

        override fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration<*>? =
            classIdToElementMap[classId] ?: session.firProvider.getFirClassifierByFqName(classId)
    }

    override fun transformDeclaration() {
        designation.ensurePhase(FirResolvePhase.SUPER_TYPES, exceptLast = true)
        val nodeInfoCollector = object : FirVisitorVoid() {
            val symbolSet = mutableSetOf<FirClassLikeSymbol<*>>()
            val classIdToElementMap = mutableMapOf<ClassId, FirClassLikeDeclaration<*>>()
            override fun visitElement(element: FirElement) {
                if (element is FirClassLikeDeclaration<*>) {
                    symbolSet.add(element.symbol)
                    classIdToElementMap[element.symbol.classId] = element
                }
                element.acceptChildren(this)
            }
        }
        nodeInfoCollector.visitElement(designation.declaration)

        val resolver = SupertypeResolver(nodeInfoCollector.symbolSet, nodeInfoCollector.classIdToElementMap)
        designation.declaration.accept(resolver, null)
        val applySupertypesTransformer = FirApplySupertypesTransformer(supertypeComputationSession)
        designation.declaration.transform<FirElement, Void?>(applySupertypesTransformer, null)
    }
}