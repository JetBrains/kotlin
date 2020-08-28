/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.element.builder

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.FirTowerDataContext
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.FirElementFinder
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.ThreadSafe
import org.jetbrains.kotlin.idea.fir.low.level.api.util.getContainingFile

/**
 * Maps [KtElement] to [FirElement]
 * Stateless, caches everything into [ModuleFileCache] & [PsiToFirCache] passed into the function
 */
@ThreadSafe
internal class FirElementBuilder(
    private val firFileBuilder: FirFileBuilder,
) {
    fun getOrBuildFirFor(
        element: KtElement,
        moduleFileCache: ModuleFileCache,
        psiToFirCache: PsiToFirCache,
        toPhase: FirResolvePhase,
    ): FirElement {
        val ktFile = element.containingKtFile
        val firFile = firFileBuilder.buildRawFirFileWithCaching(ktFile, moduleFileCache)

        val containerFir = when (val container = element.getNonLocalContainingDeclarationWithFqName()) {
            is KtDeclaration -> {
                FirElementFinder.findElementByPsiIn<FirDeclaration>(firFile, container)
                    ?: error("Declaration was not found in FIR file which was build by declaration KtFile, declaration is\n${container.getElementTextInContext()}")
            }
            null -> firFile
            else -> error("Unsupported: ${container.text}")
        }

        firFileBuilder.runCustomResolve(firFile, moduleFileCache) {
            runLazyResolveWithoutLock(containerFir, firFile, firFile.session.firIdeProvider, toPhase)
        }

        return psiToFirCache.getFir(element, containerFir, firFile)
    }

    fun lazyResolveDeclaration(
        declaration: FirDeclaration,
        moduleFileCache: ModuleFileCache,
        toPhase: FirResolvePhase
    ) {
        if (declaration.resolvePhase < toPhase) {
            val firFile = declaration.getContainingFile() ?: error("FirFile was not found for\n${declaration.render()}")
            firFileBuilder.runCustomResolve(firFile, moduleFileCache) {
                runLazyResolveWithoutLock(declaration, firFile, declaration.session.firProvider, toPhase)
            }
        }
    }

    /**
     * Should be invoked under lock
     */
    private fun runLazyResolveWithoutLock(
        firDeclarationToResolve: FirDeclaration,
        containerFirFile: FirFile,
        provider: FirProvider,
        toPhase: FirResolvePhase,
        towerDataContextForStatement: MutableMap<FirStatement, FirTowerDataContext>? = null,
    ) {
        val nonLazyPhase = minOf(toPhase, FirResolvePhase.DECLARATIONS)
        if (firDeclarationToResolve.resolvePhase < nonLazyPhase) {
            firFileBuilder.runResolveWithoutLock(containerFirFile, fromPhase = firDeclarationToResolve.resolvePhase, toPhase = nonLazyPhase)
        }
        if (toPhase <= nonLazyPhase) return
        val designation = mutableListOf<FirDeclaration>(containerFirFile)
        if (firDeclarationToResolve !is FirFile) {

            val id = when (firDeclarationToResolve) {
                is FirCallableDeclaration<*> -> {
                    firDeclarationToResolve.symbol.callableId.classId
                }
                is FirRegularClass -> {
                    firDeclarationToResolve.symbol.classId
                }
                else -> error("Unsupported: ${firDeclarationToResolve.render()}")
            }
            val outerClasses = generateSequence(id) { classId ->
                classId.outerClassId
            }.mapTo(mutableListOf()) { provider.getFirClassifierByFqName(it)!! }
            designation += outerClasses.asReversed()
            if (firDeclarationToResolve is FirCallableDeclaration<*>) {
                designation += firDeclarationToResolve
            }
        }
        if (designation.all { it.resolvePhase >= toPhase }) {
            return
        }
        val scopeSession = ScopeSession()
        val transformer = FirDesignatedBodyResolveTransformerForIDE(
            designation.iterator(), containerFirFile.session,
            scopeSession,
            implicitTypeOnly = toPhase == FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
            towerDataContextForStatement
        )
        containerFirFile.transform<FirFile, ResolutionMode>(transformer, ResolutionMode.ContextDependent)
    }

    //TODO for completion only
    fun runLazyResolveForCompletion(
        firFunction: FirFunction<*>,
        containerFirFile: FirFile,
        firIdeProvider: FirIdeProvider,
        toPhase: FirResolvePhase,
        towerDataContextForStatement: MutableMap<FirStatement, FirTowerDataContext>
    ) {
        runLazyResolveWithoutLock(firFunction, containerFirFile, firIdeProvider, toPhase, towerDataContextForStatement)
    }
}


private fun KtElement.getNonLocalContainingDeclarationWithFqName(): KtDeclaration? {
    var container = parent
    while (container != null && container !is KtFile) {
        if (container is KtDeclaration
            && (container is KtClassOrObject || container is KtDeclarationWithBody)
            && !KtPsiUtil.isLocal(container)
            && container.name != null
            && container !is KtEnumEntry
            && container.containingClassOrObject !is KtEnumEntry
        ) {
            return container
        }
        container = container.parent
    }
    return null
}