/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLDiagnostic
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.LLFirDiagnosticVisitor
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.isAutonomousElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.elementCanBeLazilyResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.*
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
        private fun findNonLocalContainer(element: PsiElement): KtElement? {
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
        return getStructureElementForContainer(container)
    }

    private fun getStructureElementForContainer(container: KtElement): FileStructureElement {
        return structureElements.getOrPut(container) { createStructureElement(container) }
    }

    private fun structureElementForOrNull(element: KtElement): FileStructureElement? {
        checkCanceled()
        return LLFirDiagnosticVisitor.suppressAndLogExceptions {
            getStructureElementFor(element)
        }
    }

    private fun structureElementForContainerOrNull(container: KtElement): FileStructureElement? {
        checkCanceled()
        return LLFirDiagnosticVisitor.suppressAndLogExceptions {
            getStructureElementForContainer(container)
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

    /**
     * Returns the [closestContainer] followed by all its own containers, up to the [KtFile]. The [FileStructureElement] of them may
     * own diagnostics reported on the element the [closestContainer] was computed for, or inside it.
     *
     * A diagnostic is not necessarily owned by the structure element of the closest non-local container of the element it is reported on:
     *
     * - Container checkers report diagnostics on their nested declarations. `MUST_BE_INITIALIZED`, for instance, is reported on a property
     *   by the containing file or class, as only the container has the control flow graph the check is based on.
     * - Control flow checkers only run on non-nested control flow graphs. A constructor, an initializer block and a property initializer are
     *   all a part of the container's graph, so diagnostics reported inside them (`UNREACHABLE_CODE`, for instance) belong to the container
     *   as well, no matter how deeply nested the reported element is.
     *
     * @see getStructureElementFor
     */
    private fun containersOf(closestContainer: KtElement): Sequence<KtElement> = generateSequence(closestContainer) { container ->
        when {
            // The file is the outermost container, so there is nothing above it
            container is KtFile -> null

            // The super type call redirection is only relevant for the requested element: a container of a container is always found by
            // walking the parents up, so [getContainerKtElement] is not needed here.
            else -> findNonLocalContainer(container.parent) ?: container.containingKtFile
        }
    }

    /**
     * Returns diagnostics reported on [element] itself, but not on its children.
     *
     * @see containersOf
     */
    fun directDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): Sequence<LLDiagnostic> = sequence {
        val closestContainer = getContainerKtElement(element, findNonLocalContainer(element))

        for (container in containersOf(closestContainer)) {
            ProgressManager.checkCanceled()

            val structureElement = getStructureElementForContainer(container)
            yieldAll(structureElement.diagnostics.directDiagnostics(filter, element))
        }
    }

    /**
     * Returns diagnostics reported on [element] and on all its children.
     *
     * Only [FileStructureElement]s which may contain diagnostics of the [element]'s subtree are computed, so requesting diagnostics for a
     * single declaration does not resolve the whole file.
     *
     * @see getStructureElementsFor
     */
    fun diagnostics(element: KtElement, filter: DiagnosticCheckerFilter): Sequence<LLDiagnostic> = sequence {
        // A structure element of the whole file cannot contain diagnostics from the outside, so there is nothing to filter out.
        val isWholeFile = element is KtFile

        for (structureElement in getStructureElementsFor(element)) {
            ProgressManager.checkCanceled()

            for (llDiagnostic in structureElement.diagnostics.diagnostics(filter)) {
                // A structure element may cover a wider piece of code than the requested element, and it may even own diagnostics reported
                // outside its own declaration (e.g., a primary constructor element owns diagnostics of the super type call). So diagnostics
                // reported outside the element have to be dropped.
                if (isWholeFile || element.isAncestor(llDiagnostic.diagnostic.psiElement, strict = false)) {
                    yield(llDiagnostic)
                }
            }
        }
    }

    internal fun getAllStructureElements(): Collection<FileStructureElement> = getStructureElementsFor(ktFile).toList()

    /**
     * Returns all [FileStructureElement]s which may contain diagnostics reported on [element] or on its children: structure elements of all
     * declarations inside [element], and structure elements of all [containers][containersOf] of [element].
     *
     * The outer containers come last and are computed lazily, as they are the most expensive structure elements: a consumer which stops early
     * never computes them.
     */
    private fun getStructureElementsFor(element: KtElement): Sequence<FileStructureElement> {
        val closestContainer = getContainerKtElement(element, findNonLocalContainer(element))

        // Sic! These structure elements are created eagerly, the closest container first and the inner ones in the document order. Creating a
        // structure element triggers the resolution of its declaration, and checkers depend on the resolution order:
        //
        // - The checker context of a nested declaration is built from the file downwards, so the file has to be resolved first.
        // - Class checkers may inspect related declarations reached through a member scope. 'FirOverrideChecker', for instance, compares the
        //   default values of an override with the ones of the base declaration, and reports on the base default value expression. An
        //   unresolved expression has no source, which makes the checker throw.
        // - Declarations may have interdependent implicit types.
        //
        // A single structure element may have several anchors (a class and its super class type reference, for instance), hence the set.
        //
        // TODO(KT-88111): compute the inner structure elements lazily as well
        val innerElements = LinkedHashSet<FileStructureElement>()
        structureElementForContainerOrNull(closestContainer)?.let(innerElements::add)

        for (anchor in collectInnerAnchorsFor(element, closestContainer)) {
            structureElementForOrNull(anchor)?.let(innerElements::add)
        }

        // Diagnostics are collected from bottom to top, so that all nested declarations are fully resolved before the outer one (KT-65562).
        // The closest container is already handled above, so only the outer ones are left. They may repeat an inner structure element, as a
        // super type call anchors the primary constructor while its own container is the class.
        val outerElements = containersOf(closestContainer)
            .drop(1)
            .mapNotNull(::structureElementForContainerOrNull)
            .filter { it !in innerElements }

        return innerElements.toList().asReversed().asSequence() + outerElements
    }

    /**
     * Collects elements inside [element] which anchor their own [FileStructureElement], in the document order.
     *
     * Nothing is collected if the [closestContainer] cannot have inner structure elements, as every declaration inside such a container is
     * local and thus belongs to the container itself. Apart from being an optimization, this avoids a pointless traversal over the whole
     * subtree of an arbitrary expression, which is the most common request.
     */
    private fun collectInnerAnchorsFor(element: KtElement, closestContainer: KtElement): List<KtElement> {
        if (!closestContainer.canHaveInnerStructureElements) {
            return emptyList()
        }

        val anchors = mutableListOf<KtElement>()

        element.accept(object : KtVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                element.acceptChildren(this)
            }

            override fun visitDeclaration(dcl: KtDeclaration) {
                anchors += dcl

                // Go down only in the case of container declaration
                if (dcl.canHaveInnerStructureElements) {
                    dcl.acceptChildren(this)
                }
            }

            override fun visitModifierList(list: KtModifierList) {
                if (elementCanBeLazilyResolved(list)) {
                    anchors += list
                }
            }

            /**
             * A super type call is split between two structure elements: the super class type reference belongs to the class, while the rest
             * of the call belongs to the primary constructor. The call is not a declaration, so it has to be anchored explicitly, and no
             * container of it can stand in: the primary constructor is a sibling of the super type list, not its parent.
             *
             * There is no need to go inside the call. The type reference half is covered by the class, which is a container of the call, and
             * every declaration inside an argument is local and thus belongs to the primary constructor anyway.
             *
             * @see getStructureKtElement
             */
            override fun visitSuperTypeCallEntry(call: KtSuperTypeCallEntry) {
                anchors += call
            }
        })

        return anchors
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

/**
 * Whether the element may contain declarations which anchor their own [FileStructureElement].
 *
 * Only declarations directly nested in a file, a class, a script or a destructuring declaration can be resolved autonomously, and only a
 * file or a class body can hold a non-local dangling modifier list. So anything nested deeper – inside a function body, a property
 * initializer or an initializer block – is local and belongs to the structure element of the enclosing declaration.
 *
 * @see org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.elementCanBeLazilyResolved
 */
private val KtElement.canHaveInnerStructureElements: Boolean
    get() = this is KtFile || this is KtClassOrObject || this is KtScript || this is KtDestructuringDeclaration
