/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.overrideImplement

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.core.insertMembersAfter
import org.jetbrains.kotlin.idea.core.moveCaretIntoGeneratedElement
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.components.KtDeclarationRendererOptions
import org.jetbrains.kotlin.idea.frontend.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.idea.frontend.api.components.RendererModifier
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.tokens.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.idea.frontend.api.tokens.hackyAllowRunningOnEdt
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.prevSiblingOfSameType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

internal abstract class KtGenerateMembersHandler : AbstractGenerateMembersHandler<KtClassMember>() {
    @OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
    override fun generateMembers(
        editor: Editor,
        classOrObject: KtClassOrObject,
        selectedElements: Collection<KtClassMember>,
        copyDoc: Boolean
    ) {
        // Using hackyAllowRunningOnEdt here because we don't want to pre-populate all possible textual overrides before user selection.
        val (commands, insertedBlocks) = hackyAllowRunningOnEdt {
            val entryMembers = analyse(classOrObject) {
                this.generateMembers(editor, classOrObject, selectedElements, copyDoc)
            }
            val insertedBlocks = insertMembersAccordingToPreferredOrder(entryMembers, editor, classOrObject)
            // Reference shortening is done in a separate analysis session because the session need to be aware of the newly generated
            // members.
            val commands = analyse(classOrObject) {
                insertedBlocks.mapNotNull { block ->
                    val declarations = block.declarations.mapNotNull { it.element }
                    val first = declarations.firstOrNull() ?: return@mapNotNull null
                    val last = declarations.last()
                    collectPossibleReferenceShortenings(first.containingKtFile, TextRange(first.startOffset, last.endOffset))
                }
            }
            commands to insertedBlocks
        }
        runWriteAction {
            commands.forEach { it.invokeShortening() }
            val project = classOrObject.project
            val codeStyleManager = CodeStyleManager.getInstance(project)
            insertedBlocks.forEach { block ->
                block.declarations.forEach { declaration ->
                    declaration.element?.let { element ->
                        codeStyleManager.reformat(
                            element
                        )
                    }
                }
            }
            insertedBlocks.firstOrNull()?.declarations?.firstNotNullResult { it.element }?.let {
                moveCaretIntoGeneratedElement(editor, it)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun KtAnalysisSession.generateMembers(
        editor: Editor,
        currentClass: KtClassOrObject,
        selectedElements: Collection<KtClassMember>,
        copyDoc: Boolean
    ): List<MemberEntry> {
        if (selectedElements.isEmpty()) return emptyList()
        val selectedMemberSymbolsAndGeneratedPsi: Map<KtCallableSymbol, KtCallableDeclaration> = selectedElements.associate {
            it.symbol to generateMember(currentClass.project, it, currentClass, copyDoc)
        }

        val classBody = currentClass.body
        val offset = editor.caretModel.offset

        // Insert members at the cursor position if the cursor is within the class body. Or, if there is no body, generate the body and put
        // stuff in it.
        if (classBody == null || isCursorInsideClassBodyExcludingBraces(classBody, offset)) {
            return selectedMemberSymbolsAndGeneratedPsi.values.map { MemberEntry.NewEntry(it) }
        }

        // Insert members at positions such that the result aligns with ordering of members in super types.
        return getMembersOrderedByRelativePositionsInSuperTypes(currentClass, selectedMemberSymbolsAndGeneratedPsi)
    }

    private fun isCursorInsideClassBodyExcludingBraces(classBody: KtClassBody, offset: Int): Boolean {
        return classBody.textRange.contains(offset)
                && classBody.lBrace?.textRange?.contains(offset) == false
                && classBody.rBrace?.textRange?.contains(offset) == false
    }

    /**
     * Given a class and some stub implementation of overridden members, output all the callable members in the desired order. For example,
     * consider the following code
     *
     * ```
     * interface Super {
     *   fun a()
     *   fun b()
     *   fun c()
     * }
     *
     * class Sub: Super {
     *   override fun b() {}
     * }
     * ```
     *
     * Now this method is invoked with `Sub` as [currentClass] and `Super.a` and `Super.c` as [newMemberSymbolsAndGeneratedPsi]. This
     * method outputs `[NewEntry(Sub.a), ExistingEntry(Sub.b), NewEntry(Sub.c)]`.
     *
     * How does this work?
     *
     * Initially we put all existing members in [currentClass] into a doubly linked list in the order they appear in the source code. Then,
     * for each new member, the algorithm finds a target node nearby which this new member should be inserted. If the algorithm fails to
     * find a desired target node, then the new member is inserted at the end.
     *
     * With the above code as an example, initially the doubly linked list contains `[ExistingEntry(Sub.b)]`. Then for `a`, the algorithm
     * somehow (explained below) finds `ExistingEntry(Sub.b)` as the target node before which the new member `a` should be inserted. So now
     * the doubly linked list contains `[NewEntry(Sub.a), ExistingEntry(Sub.b)]`. Similar steps are done for `c`.
     *
     * How does the algorithm find the target node and how does it decide whether to insert the new member before or after the target node?
     *
     * Throughout the algorithm, we maintain a map that tracks super member declarations for each member in the doubly linked list. For
     * example, initially, the map contains `{ Super.b -> ExistingEntry(Sub.b) }`
     *
     * Given a new member, the algorithm first finds the PSI that declares this member in the super class. Then it traverses all the
     * sibling members before this PSI element. With the above example, there is nothing before `Super.a`. Next it traverses all the
     * sibling members after this PSI element. With the above example, it finds `Super.b`, which exists in the map. So the algorithm now
     * knows `Sub.a` should be inserted before `Sub.b`.
     *
     * @param currentClass the class where the generated member code will be placed in
     * @param newMemberSymbolsAndGeneratedPsi the generated members to insert into the class. For each entry in the map, the key is a
     * callable symbol for an overridable member that the user has picked to override (or implement), and the value is the stub
     * implementation for the chosen symbol.
     */
    private fun KtAnalysisSession.getMembersOrderedByRelativePositionsInSuperTypes(
        currentClass: KtClassOrObject,
        newMemberSymbolsAndGeneratedPsi: Map<KtCallableSymbol, KtCallableDeclaration>
    ): List<MemberEntry> {

        // This doubly linked list tracks the preferred ordering of members.
        val sentinelHeadNode = DoublyLinkedNode<MemberEntry>()
        val sentinelTailNode = DoublyLinkedNode<MemberEntry>()
        sentinelHeadNode.append(sentinelTailNode)

        // Traverse existing members in the current class and populate
        // - a doubly linked list tracking the order
        // - a map that tracks a member (as a doubly linked list node) in the current class and its overridden members in super classes (as
        // a PSI element). This map is to allow fast look up from a super PSI element to a member entry in the current class
        val existingDeclarations = currentClass.declarations.filterIsInstance<KtCallableDeclaration>()
        val superPsiToMemberEntry = mutableMapOf<PsiElement, DoublyLinkedNode<MemberEntry>>().apply {
            for (existingDeclaration in existingDeclarations) {
                val node: DoublyLinkedNode<MemberEntry> = DoublyLinkedNode(MemberEntry.ExistingEntry(existingDeclaration))
                sentinelTailNode.prepend(node)
                val callableSymbol = existingDeclaration.getSymbol() as? KtCallableSymbol ?: continue
                for (overriddenSymbol in callableSymbol.getAllOverriddenSymbols()) {
                    put(overriddenSymbol.psi ?: continue, node)
                }
            }
        }

        // Note on implementation: here we need the original ordering defined in the source code, so we stick to PSI rather than using
        // `KtMemberScope` because the latter does not guarantee members are traversed in the original order. For example the
        // FIR implementation groups overloaded functions together.
        outer@ for ((selectedSymbol, generatedPsi) in newMemberSymbolsAndGeneratedPsi) {
            val superSymbol = selectedSymbol.originalOverriddenSymbol
            val superPsi = superSymbol?.psi
            if (superPsi == null) {
                // This normally should not happen, but we just try to play safe here.
                sentinelTailNode.prepend(DoublyLinkedNode(MemberEntry.NewEntry(generatedPsi)))
                continue
            }
            var currentPsi = superSymbol.psi?.prevSibling
            while (currentPsi != null) {
                val matchedNode = superPsiToMemberEntry[currentPsi]
                if (matchedNode != null) {
                    val newNode = DoublyLinkedNode<MemberEntry>(MemberEntry.NewEntry(generatedPsi))
                    matchedNode.append(newNode)
                    superPsiToMemberEntry[superPsi] = newNode
                    continue@outer
                }
                currentPsi = currentPsi.prevSibling
            }
            currentPsi = superSymbol.psi?.nextSibling
            while (currentPsi != null) {
                val matchedNode = superPsiToMemberEntry[currentPsi]
                if (matchedNode != null) {
                    val newNode = DoublyLinkedNode<MemberEntry>(MemberEntry.NewEntry(generatedPsi))
                    matchedNode.prepend(newNode)
                    superPsiToMemberEntry[superPsi] = newNode
                    continue@outer
                }
                currentPsi = currentPsi.nextSibling
            }
            val newNode = DoublyLinkedNode<MemberEntry>(MemberEntry.NewEntry(generatedPsi))
            superPsiToMemberEntry[superPsi] = newNode
            sentinelTailNode.prepend(newNode)
        }
        return sentinelHeadNode.toListSkippingNulls()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun insertMembersAccordingToPreferredOrder(
        symbolsInPreferredOrder: List<MemberEntry>,
        editor: Editor,
        currentClass: KtClassOrObject
    ): List<InsertedBlock> {
        if (symbolsInPreferredOrder.isEmpty()) return emptyList()
        var firstAnchor: PsiElement? = null
        if (symbolsInPreferredOrder.first() is MemberEntry.NewEntry) {
            val firstExistingEntry = symbolsInPreferredOrder.firstIsInstanceOrNull<MemberEntry.ExistingEntry>()
            if (firstExistingEntry != null) {
                firstAnchor = firstExistingEntry.psi.prevSiblingOfSameType() ?: currentClass.body?.lBrace
            }
        }

        val insertionBlocks = mutableListOf<InsertionBlock>()
        var currentAnchor = firstAnchor
        val currentBatch = mutableListOf<KtCallableDeclaration>()
        fun updateBatch() {
            if (currentBatch.isNotEmpty()) {
                insertionBlocks += InsertionBlock(currentBatch.toList(), currentAnchor)
                currentBatch.clear()
            }
        }
        for (entry in symbolsInPreferredOrder) {
            when (entry) {
                is MemberEntry.ExistingEntry -> {
                    updateBatch()
                    currentAnchor = entry.psi
                }
                is MemberEntry.NewEntry -> {
                    currentBatch += entry.psi
                }
            }
        }
        updateBatch()

        return runWriteAction {
            insertionBlocks.map { (newDeclarations, anchor) ->
                InsertedBlock(insertMembersAfter(editor, currentClass, newDeclarations, anchor = anchor))
            }
        }
    }

    private class DoublyLinkedNode<T>(val t: T? = null) {
        private var prev: DoublyLinkedNode<T>? = null
        private var next: DoublyLinkedNode<T>? = null

        fun append(node: DoublyLinkedNode<T>) {
            val next = this.next
            this.next = node
            node.prev = this
            node.next = next
            next?.prev = node
        }

        fun prepend(node: DoublyLinkedNode<T>) {
            val prev = this.prev
            this.prev = node
            node.next = this
            node.prev = prev
            prev?.next = node
        }

        @OptIn(ExperimentalStdlibApi::class)
        fun toListSkippingNulls(): List<T> {
            var current: DoublyLinkedNode<T>? = this
            return buildList {
                while (current != null) {
                    current?.let {
                        if (it.t != null) add(it.t)
                        current = it.next
                    }
                }
            }
        }
    }

    private sealed class MemberEntry {
        data class ExistingEntry(val psi: KtCallableDeclaration) : MemberEntry()
        data class NewEntry(val psi: KtCallableDeclaration) : MemberEntry()
    }

    companion object {
        val renderOption = KtDeclarationRendererOptions(
            modifiers = setOf(RendererModifier.OVERRIDE, RendererModifier.ANNOTATIONS),
            renderDeclarationHeader = false,
            renderUnitReturnType = true,
            typeRendererOptions = KtTypeRendererOptions(
                shortQualifiedNames = true,
                renderFunctionType = true,
            )
        )
    }

    /** A block of code (represented as a list of Kotlin declarations) that should be inserted at a given anchor. */
    private data class InsertionBlock(val declarations: List<KtDeclaration>, val anchor: PsiElement?)

    /** A block of generated code. The code is represented as a list of Kotlin declarations that are defined one after another. */
    private data class InsertedBlock(val declarations: List<SmartPsiElementPointer<KtDeclaration>>)
}