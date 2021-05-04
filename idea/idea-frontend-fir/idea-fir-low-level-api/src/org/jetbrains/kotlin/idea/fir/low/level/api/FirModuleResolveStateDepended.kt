/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.InternalForInline
import org.jetbrains.kotlin.idea.fir.low.level.api.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.util.containingKtFileIfAny
import org.jetbrains.kotlin.idea.fir.low.level.api.util.originalKtFile
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression

internal class FirModuleResolveStateDepended(
    private val originalState: FirModuleResolveStateImpl,
    val towerProviderBuiltUponElement: FirTowerContextProvider,
    private val ktToFirMapping: Map<KtElement, FirElement>,
) : FirModuleResolveState() {

    override val project: Project get() = originalState.project
    override val moduleInfo: IdeaModuleInfo get() = originalState.moduleInfo
    override val rootModuleSession get() = originalState.rootModuleSession
    private val fileStructureCache get() = originalState.fileStructureCache

    override fun getSessionFor(moduleInfo: IdeaModuleInfo): FirSession =
        originalState.getSessionFor(moduleInfo)

    override fun getOrBuildFirFor(element: KtElement): FirElement {
        val psi = originalState.elementBuilder.getPsiAsFirElementSource(element)

        //TODO It return invalid elements for elements with invalid code, but try to return the most closest ones
        var currentElement: PsiElement = psi
        while (currentElement !is KtFile) {
            ktToFirMapping[currentElement]?.let { return it }
            currentElement = currentElement.parent
        }

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
}