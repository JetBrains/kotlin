/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.structure

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.idea.fir.low.level.api.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import java.util.concurrent.ConcurrentHashMap

internal class FileStructure private constructor(
    private val ktFile: KtFile,
    private val firFile: FirFile,
    private val firLazyDeclarationResolver: FirLazyDeclarationResolver,
    private val firFileBuilder: FirFileBuilder,
    private val moduleFileCache: ModuleFileCache,
) {
    companion object {
        fun build(
            ktFile: KtFile,
            firLazyDeclarationResolver: FirLazyDeclarationResolver,
            firFileBuilder: FirFileBuilder,
            moduleFileCache: ModuleFileCache,
        ): FileStructure {
            val firFile = firFileBuilder.buildRawFirFileWithCaching(ktFile, moduleFileCache, preferLazyBodies = false)
            return FileStructure(ktFile, firFile, firLazyDeclarationResolver, firFileBuilder, moduleFileCache)
        }
    }

    private val firIdeProvider = firFile.moduleData.session.firIdeProvider

    private val structureElements = ConcurrentHashMap<KtAnnotated, FileStructureElement>()

    fun getStructureElementFor(element: KtElement): FileStructureElement {
        val container: KtAnnotated = element.getNonLocalContainingOrThisDeclaration() ?: element.containingKtFile
        return getStructureElementForDeclaration(container)
    }

    private fun getStructureElementForDeclaration(declaration: KtAnnotated): FileStructureElement {
        @Suppress("CANNOT_CHECK_FOR_ERASED")
        val structureElement = structureElements.compute(declaration) { _, structureElement ->
            when {
                structureElement == null -> createStructureElement(declaration)
                structureElement is ReanalyzableStructureElement<KtDeclaration, *> && !structureElement.isUpToDate() -> {
                    structureElement.reanalyze(
                        newKtDeclaration = declaration as KtDeclaration,
                        cache = moduleFileCache,
                        firLazyDeclarationResolver = firLazyDeclarationResolver,
                        firIdeProvider = firIdeProvider,
                    )
                }
                else -> structureElement
            }
        }
        return structureElement
            ?: error("FileStructureElement for was not defined for \n${declaration.getElementTextInContext()}")
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun getAllDiagnosticsForFile(diagnosticCheckerFilter: DiagnosticCheckerFilter): Collection<FirPsiDiagnostic<*>> {
        val structureElements = getAllStructureElements()

        return buildList {
            collectDiagnosticsFromStructureElements(structureElements, diagnosticCheckerFilter)
        }
    }

    private fun MutableCollection<FirPsiDiagnostic<*>>.collectDiagnosticsFromStructureElements(
        structureElements: Collection<FileStructureElement>,
        diagnosticCheckerFilter: DiagnosticCheckerFilter
    ) {
        structureElements.forEach { structureElement ->
            structureElement.diagnostics.forEach(diagnosticCheckerFilter) { diagnostics ->
                addAll(diagnostics)
            }
        }
    }

    fun getAllStructureElements(): Collection<FileStructureElement> {
        val structureElements = mutableSetOf(getStructureElementFor(ktFile))
        ktFile.accept(object : KtVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                element.acceptChildren(this)
            }

            override fun visitDeclaration(dcl: KtDeclaration) {
                val structureElement = getStructureElementFor(dcl)
                structureElements += structureElement
                if (structureElement !is ReanalyzableStructureElement<*, *>) {
                    dcl.acceptChildren(this)
                }
            }
        })

        return structureElements
    }


    private fun createDeclarationStructure(declaration: KtDeclaration): FileStructureElement {
        val firDeclaration = declaration.findSourceNonLocalFirDeclaration(
            firFileBuilder,
            firIdeProvider.symbolProvider,
            moduleFileCache,
            firFile
        )
        firLazyDeclarationResolver.lazyResolveDeclaration(
            firDeclarationToResolve = firDeclaration,
            moduleFileCache = moduleFileCache,
            scopeSession = ScopeSession(),
            toPhase = FirResolvePhase.BODY_RESOLVE,
            checkPCE = true,
        )
        return moduleFileCache.firFileLockProvider.withReadLock(firFile) {
            FileElementFactory.createFileStructureElement(firDeclaration, declaration, firFile, moduleFileCache.firFileLockProvider)
        }
    }

    private fun createStructureElement(container: KtAnnotated): FileStructureElement = when (container) {
        is KtFile -> {
            val firFile = firFileBuilder.buildRawFirFileWithCaching(ktFile, moduleFileCache, preferLazyBodies = true)
            firLazyDeclarationResolver.resolveFileAnnotations(
                firFile = firFile,
                annotations = firFile.annotations,
                moduleFileCache = moduleFileCache,
                scopeSession = ScopeSession(),
                checkPCE = true
            )
            RootStructureElement(
                firFile,
                container,
                moduleFileCache.firFileLockProvider,
            )
        }
        is KtDeclaration -> createDeclarationStructure(container)
        else -> error("Invalid container $container")
    }
}
