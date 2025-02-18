/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.LLFirDiagnosticVisitor
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.isAutonomousDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.*
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirDanglingModifierList
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import java.util.concurrent.ConcurrentHashMap

/**
 * Aggregates [KT][KtElement] -> [FIR][org.jetbrains.kotlin.fir.FirElement] mappings and diagnostics for associated [KtFile].
 *
 * For every [KtFile] we need mapping for, we have [FileStructure] which contains a tree like-structure of [FileStructureElement].
 *
 * When we want to get `KT -> FIR` mapping,
 * we [getOrPut][getStructureElementFor] [FileStructureElement] for the closest non-local declaration
 * which contains the requested [KtElement].
 *
 * Some of [FileStructureElement] can be invalidated in the case on in-block PSI modification.
 * See [invalidateElement] and [LLFirDeclarationModificationService] for details.
 *
 * The mapping is an optimization to avoid searching for the associated [FirElement][org.jetbrains.kotlin.fir.FirElement]
 * by [KtElement] as it requires deep traverse through the main element of [FileStructureElement].
 *
 * @see org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirElementBuilder
 * @see FileStructureElement
 * @see LLFirDeclarationModificationService
 */
class FileStructure constructor(
    val ktFile: KtFile,
    val firFile: FirFile,
    val moduleComponents: LLFirModuleResolveComponents,
) {
    companion object {
        fun build(
            ktFile: KtFile,
            moduleComponents: LLFirModuleResolveComponents,
        ): FileStructure {
            val firFile = moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
            return FileStructure(ktFile, firFile, moduleComponents)
        }

        /**
         * Returns [KtDeclaration] which will be used inside [getStructureElementFor].
         * `null` means that [KtElement.containingKtFile] will be used instead.
         *
         * @see getNonLocalContainingOrThisDeclaration
         */
        fun findNonLocalContainer(element: KtElement): KtDeclaration? {
            return element.getNonLocalContainingOrThisDeclaration(KtDeclaration::isAutonomousDeclaration)
        }
    }

    val firProvider = firFile.moduleData.session.firProvider

    val structureElements = ConcurrentHashMap<KtElement, FileStructureElement>()

    /**
     * Must be called only under write-lock.
     *
     * This method is responsible for "invalidation" of re-analyzable declarations.
     *
     * @see LLFirDeclarationModificationService
     * @see getNonLocalReanalyzableContainingDeclaration
     */
    fun invalidateElement(element: KtElement) {
        val container = getContainerKtElement(element, findNonLocalContainer(element))
        structureElements.remove(container)
    }

    /**
     * @return [FileStructureElement] for the closest non-local declaration which contains this [element].
     */
    fun getStructureElementFor(
        element: KtElement,
        nonLocalContainer: KtDeclaration? = findNonLocalContainer(element),
    ): FileStructureElement {
        val container = getContainerKtElement(element, nonLocalContainer)
        return structureElements.getOrPut(container) { createStructureElement(container) }
    }

    fun addStructureElementForTo(element: KtElement, result: MutableCollection<FileStructureElement>) {
        checkCanceled()
        LLFirDiagnosticVisitor.suppressAndLogExceptions {
            result += getStructureElementFor(element)
        }
    }

    fun getContainerKtElement(element: KtElement, nonLocalContainer: KtDeclaration?): KtElement {
        val declaration = getStructureKtElement(element, nonLocalContainer)
        val container: KtElement
        if (declaration != null) {
            container = declaration
        } else {
            val modifierList = PsiTreeUtil.getParentOfType(element, KtModifierList::class.java, false)
            container = if (modifierList != null && modifierList.getNextSiblingIgnoringWhitespaceAndComments() is PsiErrorElement) {
                modifierList
            } else {
                element.containingKtFile
            }
        }

        return container
    }

    fun getStructureKtElement(element: KtElement, nonLocalContainer: KtDeclaration?): KtDeclaration? {
        val container = if (nonLocalContainer?.isAutonomousDeclaration == true)
            nonLocalContainer
        else {
            nonLocalContainer?.let(::findNonLocalContainer)
        }

        val resultedContainer = when {
            container is KtClassOrObject && container.isPartOfSuperClassCall(element) -> {
                container.primaryConstructor
            }
            else -> null
        }

        return resultedContainer ?: container
    }

    fun KtClassOrObject.isPartOfSuperClassCall(element: KtElement): Boolean {
        for (entry in superTypeListEntries) {
            if (entry !is KtSuperTypeCallEntry) continue

            // the structure element for `KtTypeReference` inside super class call is class declaration and not primary constructor
            val typeReferenceIsAncestor = entry.calleeExpression.typeReference?.isAncestor(element, strict = false) == true
            if (typeReferenceIsAncestor) return false

            // the structure element for `KtSuperTypeCallEntry` is primary constructor
            if (entry.isAncestor(element, strict = false)) return true
        }

        return false
    }

    fun getAllDiagnosticsForFile(diagnosticCheckerFilter: DiagnosticCheckerFilter): List<KtPsiDiagnostic> {
        val structureElements = getAllStructureElements()
        return buildList {
            collectDiagnosticsFromStructureElements(structureElements, diagnosticCheckerFilter)
        }
    }

    fun MutableCollection<KtPsiDiagnostic>.collectDiagnosticsFromStructureElements(
        structureElements: Collection<FileStructureElement>,
        diagnosticCheckerFilter: DiagnosticCheckerFilter,
    ) {
        structureElements.forEach { structureElement ->
            ProgressManager.checkCanceled()

            structureElement.diagnostics.forEach(diagnosticCheckerFilter) { diagnostics ->
                addAll(diagnostics)
            }
        }
    }

    fun getAllStructureElements(): Collection<FileStructureElement> {
        val structureElements = mutableSetOf<FileStructureElement>()
        addStructureElementForTo(ktFile, structureElements)

        ktFile.accept(object : KtVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                element.acceptChildren(this)
            }

            override fun visitDeclaration(dcl: KtDeclaration) {
                addStructureElementForTo(dcl, structureElements)

                // Go down only in the case of container declaration
                val canHaveInnerStructure = dcl is KtClassOrObject || dcl is KtScript || dcl is KtDestructuringDeclaration
                if (canHaveInnerStructure) {
                    dcl.acceptChildren(this)
                }
            }

            override fun visitModifierList(list: KtModifierList) {
                if (list.parent == ktFile) {
                    addStructureElementForTo(list, structureElements)
                }
            }
        })

        return structureElements.toList().asReversed()
    }

    fun createDeclarationStructure(declaration: KtDeclaration): FileStructureElement {
        val firDeclaration = declaration.findSourceNonLocalFirDeclaration(firFile, firProvider)
        return FileElementFactory.createFileStructureElement(
            firDeclaration = firDeclaration,
            firFile = firFile,
            moduleComponents = moduleComponents
        )
    }

    fun createDanglingModifierListStructure(container: KtModifierList): FileStructureElement {
        val firDanglingModifierList = container.findSourceByTraversingWholeTree(
            moduleComponents.firFileBuilder,
            firFile,
        ) as? FirDanglingModifierList ?: errorWithFirSpecificEntries("No dangling modifier found", psi = container)

        firDanglingModifierList.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        return DeclarationStructureElement(firFile, firDanglingModifierList, moduleComponents)
    }

    fun createStructureElement(container: KtElement): FileStructureElement = when {
        container is KtCodeFragment -> {
            val firCodeFragment = firFile.codeFragment
            firCodeFragment.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

            DeclarationStructureElement(firFile, firCodeFragment, moduleComponents)
        }

        container is KtFile -> {
            val firFile = moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
            firFile.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE.previous)

            RootStructureElement(firFile, moduleComponents)
        }

        container is KtDeclaration -> createDeclarationStructure(container)
        container is KtModifierList && container.getNextSiblingIgnoringWhitespaceAndComments() is PsiErrorElement -> {
            createDanglingModifierListStructure(container)
        }
        else -> errorWithAttachment("Invalid container ${container::class}") {
            withPsiEntry("container", container)
        }
    }
}
