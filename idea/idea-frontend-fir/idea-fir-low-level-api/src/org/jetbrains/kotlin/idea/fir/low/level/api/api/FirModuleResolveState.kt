/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import org.jetbrains.annotations.TestOnly
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.FirTowerDataContext
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.InternalForInline
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression

abstract class FirModuleResolveState {
    abstract val project: Project

    abstract val rootModuleSession: FirSession

    abstract val moduleInfo: IdeaModuleInfo

    internal abstract fun getSessionFor(moduleInfo: IdeaModuleInfo): FirSession

    internal abstract fun getOrBuildFirFor(element: KtElement): FirElement

    internal abstract fun getFirFile(ktFile: KtFile): FirFile

    internal abstract fun isFirFileBuilt(ktFile: KtFile): Boolean

    internal abstract fun getDiagnostics(element: KtElement): List<Diagnostic>

    internal abstract fun collectDiagnosticsForFile(ktFile: KtFile): Collection<Diagnostic>

    @TestOnly
    internal abstract fun getBuiltFirFileOrNull(ktFile: KtFile): FirFile?

    @InternalForInline
    abstract fun findNonLocalSourceFirDeclaration(
        ktDeclaration: KtDeclaration,
    ): FirDeclaration

    @InternalForInline
    abstract fun findSourceFirDeclaration(
        ktDeclaration: KtDeclaration,
    ): FirDeclaration

    @InternalForInline
    abstract fun findSourceFirDeclaration(
        ktDeclaration: KtLambdaExpression,
    ): FirDeclaration


    // todo temporary, used only in completion
    internal abstract fun recordPsiToFirMappingsForCompletionFrom(fir: FirDeclaration, firFile: FirFile, ktFile: KtFile)

    internal abstract fun <D : FirDeclaration> resolvedFirToPhase(declaration: D, toPhase: FirResolvePhase): D

    // todo temporary, used only in completion
    internal abstract fun lazyResolveDeclarationForCompletion(
        firFunction: FirDeclaration,
        containerFirFile: FirFile,
        firIdeProvider: FirProvider,
        toPhase: FirResolvePhase,
    )

    internal abstract fun getFirFile(declaration: FirDeclaration, cache: ModuleFileCache): FirFile?

    abstract fun getTowerDataContextForElement(element: KtElement): FirTowerDataContext?
}

fun FirModuleResolveState.getTowerDataContextUnsafe(element: KtElement): FirTowerDataContext =
    getTowerDataContextForElement(element) ?: error("No context for ${element.getElementTextInContext()}")