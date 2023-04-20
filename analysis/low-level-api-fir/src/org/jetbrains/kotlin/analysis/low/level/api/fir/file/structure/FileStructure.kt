/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findSourceByTraversingWholeTree
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.analysis.utils.printer.getElementTextInContext
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirDanglingModifierList
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.canBePartOfParentDeclaration
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor

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

    private val structureElements = ConcurrentHashMap<KtElement, FileStructureElement>()

    fun getStructureElementFor(element: KtElement): FileStructureElement {
        val declaration = getStructureKtElement(element)
        val container: KtElement
        if (declaration != null) {
            container = declaration
        } else {
            val modifierList = PsiTreeUtil.getParentOfType(element, KtModifierList::class.java, false)
            container = if (modifierList != null && modifierList.nextSibling is PsiErrorElement) {
                modifierList
            } else {
                element.containingKtFile
            }
        }

        return getStructureElementForDeclaration(container)
    }

    private fun getStructureKtElement(element: KtElement): KtDeclaration? {
        val container = element.getNonLocalContainingOrThisDeclaration {
            !it.canBePartOfParentDeclaration
        }

        val resultedContainer = when {
            container is KtClassOrObject && container.isInsideSuperClassCall(element) -> {
                container.primaryConstructor
            }
            else -> null
        }

        return resultedContainer ?: container
    }

    private fun KtClassOrObject.isInsideSuperClassCall(element: KtElement): Boolean {
        for (entry in superTypeListEntries) {
            if (entry is KtSuperTypeCallEntry && entry.isAncestor(element, strict = false)) return true
        }

        return false
    }

    private fun getStructureElementForDeclaration(declaration: KtElement): FileStructureElement {
        @Suppress("CANNOT_CHECK_FOR_ERASED")
        val structureElement = structureElements.compute(declaration) { _, structureElement ->
            when {
                structureElement == null -> createStructureElement(declaration)
                structureElement is ReanalyzableStructureElement<KtDeclaration, *> && !structureElement.isUpToDate() -> {
                    structureElement.reanalyze(newKtDeclaration = declaration as KtDeclaration)
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

            override fun visitModifierList(list: KtModifierList) {
                if (list.parent == ktFile) {
                    structureElements += getStructureElementFor(list)
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

        firDeclaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        if (firDeclaration is FirPrimaryConstructor) {
            firDeclaration.valueParameters.forEach { parameter ->
                parameter.correspondingProperty?.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            }
        }

        return FileElementFactory.createFileStructureElement(
            firDeclaration = firDeclaration,
            ktDeclaration = declaration,
            firFile = firFile,
            moduleComponents = moduleComponents
        )
    }

    private fun createDanglingModifierListStructure(container: KtElement): FileStructureElement {
        val firDanglingModifierList = container.findSourceByTraversingWholeTree(
            moduleComponents.firFileBuilder,
            firFile,
        ) as? FirDanglingModifierList ?: errorWithFirSpecificEntries("No dangling modifier found", psi = container)

        firDanglingModifierList.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        return DanglingTopLevelModifierListStructureElement(firFile, firDanglingModifierList, moduleComponents, container.containingKtFile)
    }

    private fun createStructureElement(container: KtElement): FileStructureElement = when {
        container is KtFile -> {
            val firFile = moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
            firFile.lazyResolveToPhase(FirResolvePhase.IMPORTS)

            moduleComponents.firModuleLazyDeclarationResolver.lazyResolve(
                target = firFile.annotationsContainer,
                scopeSession = moduleComponents.scopeSessionProvider.getScopeSession(),
                FirResolvePhase.BODY_RESOLVE,
            )

            RootStructureElement(firFile, container, moduleComponents)
        }
        container is KtDeclaration -> createDeclarationStructure(container)
        container is KtModifierList && container.nextSibling is PsiErrorElement -> createDanglingModifierListStructure(container)
        else -> error("Invalid container $container")
    }
}
