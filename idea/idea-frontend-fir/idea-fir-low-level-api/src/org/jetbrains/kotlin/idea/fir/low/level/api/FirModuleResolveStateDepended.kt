/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.FirTowerDataContext
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.InternalForInline
import org.jetbrains.kotlin.idea.fir.low.level.api.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getClosestAvailableParentContext
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.FirElementsRecorder
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.containingKtFileIfAny
import org.jetbrains.kotlin.idea.fir.low.level.api.util.originalKtFile
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression

internal class FirModuleResolveStateDepended(
    dependencyFirDeclaration: FirDeclaration,
    originalFirFile: FirFile,
    private val originalState: FirModuleResolveStateImpl,
) : FirModuleResolveState() {

    override val project: Project get() = originalState.project
    override val moduleInfo: IdeaModuleInfo get() = originalState.moduleInfo
    override val rootModuleSession get() = originalState.rootModuleSession
    private val fileStructureCache = originalState.fileStructureCache
    private val completionMapping = mutableMapOf<KtElement, FirElement>()
    private val collector = FirTowerDataContextCollector()

    override fun getSessionFor(moduleInfo: IdeaModuleInfo): FirSession =
        originalState.getSessionFor(moduleInfo)

    override fun getOrBuildFirFor(element: KtElement): FirElement {
        val psi = originalState.elementBuilder.getPsiAsFirElementSource(element)
        synchronized(completionMapping) { completionMapping[psi] }?.let { return it }
        return originalState.elementBuilder.getOrBuildFirFor(
            element,
            originalState.firFileBuilder,
            originalState.rootModuleSession.cache,
            fileStructureCache,
        )
    }

    override fun getFirFile(ktFile: KtFile): FirFile =
        originalState.getFirFile(ktFile)

    override fun <D : FirDeclaration> resolvedFirToPhase(declaration: D, toPhase: FirResolvePhase): D =
        originalState.resolvedFirToPhase(declaration, toPhase)

    override fun getFirFile(declaration: FirDeclaration, cache: ModuleFileCache): FirFile? {
        val ktFile = declaration.containingKtFileIfAny ?: return null
        cache.getCachedFirFile(ktFile)?.let { return it }
        ktFile.originalKtFile?.let(cache::getCachedFirFile)?.let { return it }
        return null
    }

    override fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<FirPsiDiagnostic<*>> =
        TODO("Diagnostics are not implemented for depended state")

    override fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<FirPsiDiagnostic<*>> =
        TODO("Diagnostics are not implemented for depended state")

    @OptIn(InternalForInline::class)
    override fun findSourceFirDeclaration(ktDeclaration: KtLambdaExpression): FirDeclaration =
        originalState.findSourceFirDeclaration(ktDeclaration)

    @OptIn(InternalForInline::class)
    override fun findSourceFirDeclaration(ktDeclaration: KtDeclaration): FirDeclaration =
        originalState.findSourceFirDeclaration(ktDeclaration)

    override fun getTowerDataContextForElement(element: KtElement): FirTowerDataContext? =
        collector.getClosestAvailableParentContext(element) ?: originalState.getTowerDataContextForElement(element)

    init {
        originalState.firFileBuilder.runCustomResolveWithPCECheck(originalFirFile, rootModuleSession.cache) {
            originalState.firLazyDeclarationResolver.runLazyResolveWithoutLock(
                dependencyFirDeclaration,
                rootModuleSession.cache,
                originalFirFile,
                originalFirFile.session.firIdeProvider,
                fromPhase = dependencyFirDeclaration.resolvePhase,
                toPhase = FirResolvePhase.BODY_RESOLVE,
                towerDataContextCollector = collector,
                checkPCE = true
            )
        }

        synchronized(completionMapping) { dependencyFirDeclaration.accept(FirElementsRecorder(), completionMapping) }
    }
}