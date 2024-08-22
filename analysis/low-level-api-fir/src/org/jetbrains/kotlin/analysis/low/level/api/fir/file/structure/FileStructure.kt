/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

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
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.FirDanglingModifierList
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
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

    /**
     * Must be called only under write-lock.
     *
     * This method is responsible for "invalidation" of re-analyzable declarations.
     *
     * @see LLFirDeclarationModificationService
     * @see getNonLocalReanalyzableContainingDeclaration
     */
    fun invalidateElement(element: KtElement) {
        val container = getContainerKtElement(element)
        structureElements.remove(container)
    }

    /**
     * @return [FileStructureElement] for the closest non-local declaration which contains this [element].
     */
    fun getStructureElementFor(element: KtElement): FileStructureElement {
        val container = getContainerKtElement(element)
        return structureElements.getOrPut(container) { createStructureElement(container) }
    }

    private fun addStructureElementForTo(element: KtElement, result: MutableCollection<FileStructureElement>) {
        checkCanceled()
        LLFirDiagnosticVisitor.suppressAndLogExceptions {
            result += getStructureElementFor(element)
        }
    }

    private fun getContainerKtElement(element: KtElement): KtElement {
        val declaration = getStructureKtElement(element)
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

    private fun getStructureKtElement(element: KtElement): KtDeclaration? {
        val container = element.getNonLocalContainingOrThisDeclaration {
            it.isAutonomousDeclaration
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

    private fun MutableCollection<KtPsiDiagnostic>.collectDiagnosticsFromStructureElements(
        structureElements: Collection<FileStructureElement>,
        diagnosticCheckerFilter: DiagnosticCheckerFilter,
    ) {
        structureElements.forEach { structureElement ->
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

    private fun createDeclarationStructure(declaration: KtDeclaration): FileStructureElement {
        val firDeclaration = declaration.findSourceNonLocalFirDeclaration(
            firFile,
            firProvider,
        )

        firDeclaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        if (firDeclaration is FirPrimaryConstructor) {
            firDeclaration.valueParameters.forEach { parameter ->
                parameter.correspondingProperty?.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            }
        }

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

    private fun createStructureElement(container: KtElement): FileStructureElement = when {
        container is KtCodeFragment -> {
            val firCodeFragment = firFile.codeFragment
            firCodeFragment.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

            DeclarationStructureElement(firFile, firCodeFragment, moduleComponents)
        }
        container is KtFile -> {
            val firFile = moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
            firFile.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

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
