/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.LLFirDiagnosticVisitor
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.isAutonomousElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.elementCanBeLazilyResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.*
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirDanglingModifierList
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import java.util.concurrent.ConcurrentHashMap

/**
 * Aggregates [KT][KtElement] -> [FIR][org.jetbrains.kotlin.fir.FirElement] mappings and diagnostics for the associated [KtFile].
 *
 * For every [KtFile] we need a mapping for, we have a [FileStructure] which contains a tree-like structure of [FileStructureElement]s.
 *
 * When we want to get a `KT -> FIR` mapping,
 * we [getOrPut][getStructureElementFor] a [FileStructureElement] for the closest non-local element (usually a declaration)
 * which contains the requested [KtElement].
 *
 * Some [FileStructureElement]s can be invalidated in case of an in-block PSI modification.
 * See [invalidateElement] and [LLFirDeclarationModificationService] for details.
 *
 * The mapping is an optimization to avoid searching for the associated [FirElement][org.jetbrains.kotlin.fir.FirElement]
 * by a [KtElement], as it requires a deep traversal through the main element of [FileStructureElement].
 *
 * @see org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirElementBuilder
 * @see FileStructureElement
 * @see LLFirDeclarationModificationService
 */
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

        /**
         * Returns [KtElement] which will be used inside [getStructureElementFor].
         * `null` means that [KtElement.containingKtFile] will be used instead.
         *
         * @see getNonLocalContainingOrThisElement
         */
        private fun findNonLocalContainer(element: KtElement): KtElement? {
            return element.getNonLocalContainingOrThisElement(predicate = KtElement::isAutonomousElement)
        }
    }

    private val firProvider = firFile.moduleData.session.firProvider

    private val structureElements = ConcurrentHashMap<KtElement, FileStructureElement>()

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
     * @return [FileStructureElement] for the closest non-local element which contains this [element].
     */
    fun getStructureElementFor(
        element: KtElement,
        nonLocalContainer: KtElement? = findNonLocalContainer(element),
    ): FileStructureElement {
        val container = getContainerKtElement(element, nonLocalContainer)
        return structureElements.getOrPut(container) { createStructureElement(container) }
    }

    private fun addStructureElementForTo(element: KtElement, result: MutableCollection<FileStructureElement>) {
        checkCanceled()
        LLFirDiagnosticVisitor.suppressAndLogExceptions {
            result += getStructureElementFor(element)
        }
    }

    private fun getContainerKtElement(element: KtElement, nonLocalContainer: KtElement?): KtElement {
        return getStructureKtElement(element, nonLocalContainer) ?: element.containingKtFile
    }

    private fun getStructureKtElement(element: KtElement, nonLocalContainer: KtElement?): KtElement? {
        val container = if (nonLocalContainer?.isAutonomousElement == true)
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

    private fun KtClassOrObject.isPartOfSuperClassCall(element: KtElement): Boolean {
        for (entry in superTypeListEntries) {
            if (entry !is KtSuperTypeCallEntry) continue

            // the structure element for `KtTypeReference` inside the super class call is a class declaration and not a primary constructor
            val typeReferenceIsAncestor = entry.calleeExpression.typeReference?.isAncestor(element, strict = false) == true
            if (typeReferenceIsAncestor) return false

            // the structure element for `KtSuperTypeCallEntry` is a primary constructor
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

    private fun MutableCollection<KtPsiDiagnostic>.collectDiagnosticsFromStructureElements(
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
                if (elementCanBeLazilyResolved(list, codeFragmentAware = false)) {
                    addStructureElementForTo(list, structureElements)
                }
            }
        })

        return structureElements.toList().asReversed()
    }

    private fun createRootStructure(): RootStructureElement {
        val firFile = moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
        firFile.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE.previous)
        return RootStructureElement(firFile, moduleComponents)
    }

    private fun createCodeFragmentStructure(): DeclarationStructureElement {
        val firCodeFragment = firFile.codeFragment
        firCodeFragment.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        return DeclarationStructureElement(firFile, firCodeFragment, moduleComponents)
    }

    private fun createDeclarationStructure(declaration: KtDeclaration): FileStructureElement {
        val firDeclaration = declaration.findSourceNonLocalFirDeclaration(firFile, firProvider)
        return FileElementFactory.createFileStructureElement(
            firDeclaration = firDeclaration,
            firFile = firFile,
            moduleComponents = moduleComponents
        )
    }

    private fun createDanglingModifierListStructure(container: KtModifierList): FileStructureElement {
        val firDanglingModifierList = container.findSourceByTraversingWholeTree(
            moduleComponents.firFileBuilder,
            firFile,
        ) as? FirDanglingModifierList ?: errorWithFirSpecificEntries("No dangling modifier found", psi = container)

        firDanglingModifierList.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        return DeclarationStructureElement(firFile, firDanglingModifierList, moduleComponents)
    }

    private fun createStructureElement(container: KtElement): FileStructureElement = when (container) {
        is KtCodeFragment -> createCodeFragmentStructure()
        is KtFile -> createRootStructure()
        is KtDeclaration -> createDeclarationStructure(container)
        is KtModifierList -> createDanglingModifierListStructure(container)
        else -> errorWithAttachment("Invalid container ${container::class}") {
            withPsiEntry("container", container)
        }
    }
}
