/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.InternalForInline
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSourcesSession
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

    /**
     * Build fully resolved FIR node for requested element.
     * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase, use
     * @see tryGetCachedFirFile to get [FirFile] in undefined phase
     */
    internal abstract fun getOrBuildFirFor(element: KtElement): FirElement

    /**
     * Get or build or get cached [FirFile] for requested file in undefined phase
     */
    internal abstract fun getOrBuildFirFile(ktFile: KtFile): FirFile

    /**
     * Try get [FirFile] from the cache in undefined phase
     */
    internal abstract fun tryGetCachedFirFile(declaration: FirDeclaration, cache: ModuleFileCache): FirFile?

    internal abstract fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<FirPsiDiagnostic<*>>

    internal abstract fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<FirPsiDiagnostic<*>>

    internal inline fun <D : FirDeclaration, R> withLock(declaration: D, declarationLockType: DeclarationLockType, action: (D) -> R): R {
        val originalDeclaration = (declaration as? FirCallableDeclaration<*>)?.unwrapFakeOverrides() ?: declaration
        val session = originalDeclaration.moduleData.session
        return when {
            originalDeclaration.origin == FirDeclarationOrigin.Source && session is FirIdeSourcesSession -> {
                val cache = session.cache
                val file = tryGetCachedFirFile(declaration, cache)
                    ?: error("Fir file was not found for\n${declaration.render()}\n${(declaration.psi as? KtElement)?.getElementTextInContext()}")
                cache.firFileLockProvider.withLock(file, declarationLockType) { action(declaration) }
            }
            else -> action(declaration)
        }
    }

    @InternalForInline
    abstract fun findSourceFirDeclaration(
        ktDeclaration: KtDeclaration,
    ): FirDeclaration

    @InternalForInline
    abstract fun findSourceFirDeclaration(
        ktDeclaration: KtLambdaExpression,
    ): FirDeclaration

    internal abstract fun <D : FirDeclaration> resolveFirToPhase(declaration: D, toPhase: FirResolvePhase): D
}