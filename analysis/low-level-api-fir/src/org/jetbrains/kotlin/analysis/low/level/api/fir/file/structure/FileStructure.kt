/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.analysis.utils.printer.getElementTextInContext
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.psi.*
import java.util.concurrent.ConcurrentHashMap

internal class FileStructure private constructor(
    private val ktFile: KtFile,
    private val firFile: FirFile,
    private val moduleComponents: LLFirModuleResolveComponents,
) {
    companion object {
        fun build(
            ktFile: KtFile,
            moduleComponents: LLFirModuleResolveComponents,
        ): FileStructure {
            val firFile = moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
            return FileStructure(ktFile, firFile, moduleComponents)
        }
    }

    private val firProvider = firFile.moduleData.session.firProvider

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
                    structureElement.reanalyze(newKtDeclaration = declaration as KtDeclaration,)
                }
                else -> structureElement
            }
        }
        return structureElement
            ?: error("FileStructureElement for was not defined for \n${declaration.getElementTextInContext()}")
    }

    fun getAllDiagnosticsForFile(diagnosticCheckerFilter: DiagnosticCheckerFilter): Collection<KtPsiDiagnostic> {
        val structureElements = getAllStructureElements()

        return buildList {
            collectDiagnosticsFromStructureElements(structureElements, diagnosticCheckerFilter)
        }
    }

    private fun MutableCollection<KtPsiDiagnostic>.collectDiagnosticsFromStructureElements(
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
            moduleComponents.firFileBuilder,
            firProvider,
            firFile
        )
        moduleComponents.firModuleLazyDeclarationResolver.lazyResolveDeclaration(
            firDeclarationToResolve = firDeclaration,
            scopeSession = moduleComponents.scopeSessionProvider.getScopeSession(),
            toPhase = FirResolvePhase.BODY_RESOLVE,
            checkPCE = true,
        )
        return FileElementFactory.createFileStructureElement(
            firDeclaration = firDeclaration,
            ktDeclaration = declaration,
            firFile = firFile,
            moduleComponents = moduleComponents
        )
    }

    private fun createStructureElement(container: KtAnnotated): FileStructureElement = when (container) {
        is KtFile -> {
            val firFile = moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
            moduleComponents.firModuleLazyDeclarationResolver.resolveFileAnnotations(
                firFile = firFile,
                annotations = firFile.annotations,
                scopeSession = moduleComponents.scopeSessionProvider.getScopeSession(),
                checkPCE = true
            )
            RootStructureElement(firFile, container, moduleComponents)
        }
        is KtDeclaration -> createDeclarationStructure(container)
        else -> error("Invalid container $container")
    }
}
