/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.projectStructure

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.ASTStructure
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.ThreeState
import com.intellij.util.containers.addIfNotNull
import com.intellij.util.diff.DiffTree
import com.intellij.util.diff.DiffTreeChangeBuilder
import com.intellij.util.diff.ShallowNodeComparator
import org.jetbrains.kotlin.analysis.api.platform.modification.KaElementModificationType
import org.jetbrains.kotlin.analysis.api.platform.modification.KaSourceModificationLocality
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionModeProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.copyOrigin
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLFirDeclarationModificationService
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.psi.psiUtil.textRangeWithoutComments
import org.jetbrains.kotlin.psi.stubs.*
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

/**
 * Calculates [KaDanglingFileResolutionMode] for the given [KtFile] based purely on its content.
 *
 * Initially, the calculation is done by comparing top-level stubs of the original and the copy files.
 * If they are not equal, returns [KaDanglingFileResolutionMode.PREFER_SELF].
 * Otherwise, returns [KaDanglingFileResolutionMode.IGNORE_SELF].
 *
 * In some cases when the stubs are equal, the comparator needs to gather more information on the nature of the changes.
 * In this case, the computation is done by reusing [LLFirDeclarationModificationService] which handles PSI events in the IDE.
 * To emulate these tree change events, [DiffTree] is run on the AST trees of both the original and the copy files.
 * This [DiffTree] then reports deletions / insertions / replacements
 * to the [LLFirDeclarationModificationService] based on a simple PSI comparator.
 * If any Out-of-block modification was detected, returns [KaDanglingFileResolutionMode.PREFER_SELF].
 * Otherwise, returns [KaDanglingFileResolutionMode.IGNORE_SELF].
 */
internal class KaFirDanglingFileResolutionModeProvider : KaDanglingFileResolutionModeProvider {
    @OptIn(KtImplementationDetail::class)
    override fun calculateMode(file: KtFile): KaDanglingFileResolutionMode {
        return file.getOrComputeMode {
            computeModeNonCached(file)
        }
    }

    @OptIn(KtImplementationDetail::class)
    private fun computeModeNonCached(file: KtFile): KaDanglingFileResolutionMode {
        val originalFile = file.copyOrigin as? KtFile ?: return KaDanglingFileResolutionMode.PREFER_SELF
        val originalFileStub = originalFile.calcStubTree().root as? KotlinFileStub ?: return KaDanglingFileResolutionMode.PREFER_SELF
        val copyFileStub = file.calcStubTree().root as? KotlinFileStub ?: return KaDanglingFileResolutionMode.PREFER_SELF
        val originalStubList = getFlatTopLevelStubList(originalFileStub)
        val copyStubList = getFlatTopLevelStubList(copyFileStub)

        if (originalStubList.size != copyStubList.size) {
            /**
             * Even though stubs are also created for local declarations,
             * we filter them out in [getFlatTopLevelStubList].
             * Hence, it's safe to assume that [originalStubList] and [copyStubList] contain only top-level declaration
             * so that we can compare these lists by their size.
             */
            return KaDanglingFileResolutionMode.PREFER_SELF
        }
        val originalToCopyStubs = originalStubList.zip(copyStubList)
        if (originalToCopyStubs.any { !it.first.isEquivalentTo(it.second) }) {
            return KaDanglingFileResolutionMode.PREFER_SELF
        }

        /**
         * Unfortunately, stubs don't catch all the vital info needed for comparison.
         * E.g., stubs contain no information about property / function initializers.
         * So stubs for the following two functions are equal:
         *
         * ```kotlin
         * fun foo() = 1
         * fun foo() = ""
         * ```
         *
         * That's why we need to run the PSI-based diff in some specific cases.
         */
        val foundOutOfBlockByPsiCheck = originalToCopyStubs.any { (originalStub, copyStub) ->
            if (originalStub.shouldRunPsiCheck()) {
                calculateModePsiBased(originalStub.psi, copyStub.psi) == KaDanglingFileResolutionMode.PREFER_SELF
            } else {
                false
            }
        }

        return when {
            foundOutOfBlockByPsiCheck -> KaDanglingFileResolutionMode.PREFER_SELF
            else -> KaDanglingFileResolutionMode.IGNORE_SELF
        }
    }

    /**
     * Performs a simple BFS on [stub] and returns a plain list of all children stub elements
     * that should be considered during the comparison.
     *
     * @see shouldAddChild
     */
    @OptIn(KtImplementationDetail::class)
    private fun getFlatTopLevelStubList(stub: KotlinFileStub): List<KotlinStubElement<*>> {
        val stubList: MutableList<StubElement<*>?> = mutableListOf(stub)
        val result: MutableList<KotlinStubElement<*>> = mutableListOf()
        while (stubList.isNotEmpty()) {
            val stub = stubList.removeLast() as? KotlinStubElement<*> ?: continue
            stub.childrenStubs.filterTo(stubList) { stub.shouldAddChild(it) }
            result.addIfNotNull(stub)
        }
        return result
    }

    /**
     * Returns whether [child] of [this] should be considered during the stub BFS.
     *
     * @see getFlatTopLevelStubList
     */
    private fun StubElement<*>.shouldAddChild(child: StubElement<*>): Boolean {
        return when (this) {
            // Ignore everything in accessors, this will be handled by the PSI check
            is KotlinPropertyAccessorStub -> false
            // Ignore nested classes / callables in callable stubs
            is KotlinCallableStubBase<*> -> child !is KotlinCallableStubBase<*> && child !is KotlinClassifierStub
            // Ignore everything in class initializers
            is KotlinPlaceHolderStub<*> if elementType == KtStubElementTypes.CLASS_INITIALIZER -> false
            else -> true
        }
    }

    @OptIn(KtImplementationDetail::class)
    private fun KotlinStubElement<*>.shouldRunPsiCheck(): Boolean = when (this) {
        // Properties with no specified return type, different initializers / delegates might affect the return type
        is KotlinPropertyStub if !hasReturnTypeRef -> hasDelegate || hasInitializer
        // Functions with expression body (i.e. fun foo() = expr), different initializers might affect the return type
        is KotlinFunctionStub if !hasNoExpressionBody -> true
        // Class init blocks, most changes inside them are considered OOBM
        is KotlinPlaceHolderStub<*> -> elementType == KtStubElementTypes.CLASS_INITIALIZER
        // Property accessors, the code insight accessors might affect backing field instantiation
        is KotlinPropertyAccessorStub -> true
        else -> false
    }

    /**
     * Performs an AST based diff between [originalPsi] and [copyPsi] and returns the resulting [KaDanglingFileResolutionMode].
     * Done by reusing [LLFirDeclarationModificationService] which handles PSI tree change events in the IDE.
     */
    @OptIn(LLFirInternals::class)
    private fun calculateModePsiBased(originalPsi: KtElement, copyPsi: KtElement): KaDanglingFileResolutionMode {
        val originalNode = ASTStructure(originalPsi.node)
        val copyNode = ASTStructure(copyPsi.node)

        val modificationService = LLFirDeclarationModificationService.getInstance(originalPsi.project)
        val consumer = Consumer(modificationService)
        DiffTree.diff(
            /* oldTree = */ originalNode,
            /* newTree = */ copyNode,
            /* comparator = */ NodeComparator(shouldStop = { consumer.mode == KaDanglingFileResolutionMode.PREFER_SELF }),
            /* consumer = */ consumer,
            /* oldText = */ originalPsi.text
        )
        return consumer.mode
    }

    /**
     * Compares two AstNodes based on their properties.
     *
     * If [shouldStop] returns true, the comparator will stop comparing provided elements and will start considering them equal by default.
     * This way, we can avoid unnecessary computations when OOBM was already found.
     */
    private class NodeComparator(private val shouldStop: () -> Boolean) : ShallowNodeComparator<ASTNode, ASTNode> {
        override fun deepEqual(oldNode: ASTNode, newNode: ASTNode): ThreeState {
            if (shouldStop()) {
                return ThreeState.YES
            }

            val oldNodePsi = oldNode.psi
            val newNodePsi = newNode.psi

            if (oldNode.elementType != newNode.elementType) return ThreeState.NO
            if (oldNodePsi.isWhitespaceOrComment() && newNodePsi.isWhitespaceOrComment()) return ThreeState.YES
            if (oldNodePsi is LeafPsiElement != newNodePsi is LeafPsiElement) return ThreeState.NO
            if (oldNodePsi !is LeafPsiElement) return ThreeState.UNSURE
            if (oldNodePsi.textRangeWithoutComments.isEmpty && newNodePsi.textRangeWithoutComments.isEmpty) return ThreeState.YES
            if (oldNodePsi.textMatches(newNodePsi)) return ThreeState.YES

            return ThreeState.UNSURE
        }

        override fun typesEqual(oldNode: ASTNode, newNode: ASTNode): Boolean {
            if (shouldStop()) {
                return true
            }
            return oldNode.elementType == newNode.elementType
        }

        override fun hashCodesEqual(oldNode: ASTNode, newNode: ASTNode): Boolean {
            if (shouldStop()) {
                return true
            }
            return oldNode.hashCode() == newNode.hashCode()
        }
    }

    /**
     * Handles modification events from [DiffTree] and passes them to [modificationService] to detect out-of-block modifications.
     */
    @OptIn(LLFirInternals::class)
    private class Consumer(private val modificationService: LLFirDeclarationModificationService) : DiffTreeChangeBuilder<ASTNode, ASTNode> {
        var mode: KaDanglingFileResolutionMode = KaDanglingFileResolutionMode.IGNORE_SELF

        override fun nodeReplaced(oldChild: ASTNode, newChild: ASTNode) {
            if (mode == KaDanglingFileResolutionMode.PREFER_SELF) {
                return
            }

            val oldNodePsi = oldChild.psi
            val newNodePsi = newChild.psi

            val changeType =
                calculateChangeType(newNodePsi, KaElementModificationType.ElementReplaced(oldNodePsi))
            if (changeType is KaSourceModificationLocality.OutOfBlock) {
                mode = KaDanglingFileResolutionMode.PREFER_SELF
                return
            }
        }

        override fun nodeDeleted(oldParent: ASTNode, oldNode: ASTNode) {
            if (mode == KaDanglingFileResolutionMode.PREFER_SELF) {
                return
            }

            val oldNodePsi = oldNode.psi
            val changeType =
                calculateChangeType(oldParent.psi, KaElementModificationType.ElementRemoved(oldNodePsi))
            if (changeType is KaSourceModificationLocality.OutOfBlock) {
                if (oldNodePsi.isWhitespaceOrComment()
                    && !oldNodePsi.isSeparatingWhitespaceOrComment()
                ) {
                    /**
                     * If the deleted node is whitespace / comment that didn't separate other nodes,
                     * we can be sure that the deletion doesn't affect the resolution.
                     *
                     * This logic cannot be incorporated into [LLFirDeclarationModificationService]
                     * because the deleted element in PSI events is detached from the file tree.
                     * So it's impossible to check whether the old node used to separate some nodes.
                     */
                    return
                }

                mode = KaDanglingFileResolutionMode.PREFER_SELF
            }
        }

        override fun nodeInserted(oldParent: ASTNode, newNode: ASTNode, pos: Int) {
            if (mode == KaDanglingFileResolutionMode.PREFER_SELF) {
                return
            }

            val changeType =
                calculateChangeType(newNode.psi, KaElementModificationType.ElementAdded)
            if (changeType is KaSourceModificationLocality.OutOfBlock) {
                mode = KaDanglingFileResolutionMode.PREFER_SELF
            }
        }

        @OptIn(LLFirInternals::class)
        private fun calculateChangeType(element: PsiElement, modificationType: KaElementModificationType): KaSourceModificationLocality {
            return modificationService.detectLocality(element, modificationType)
        }

        /**
         * A whitespace / comment element is considered to be separating
         * if it separates other non-whitespace / non-comment elements.
         */
        private fun PsiElement.isSeparatingWhitespaceOrComment(): Boolean {
            if (!this.isWhitespaceOrComment()) {
                return false
            }
            val prevLeaf = this.prevLeaf(skipEmptyElements = true) ?: return false
            val nextLeaf = this.nextLeaf(skipEmptyElements = true) ?: return false
            return !prevLeaf.isWhitespaceOrComment() && !nextLeaf.isWhitespaceOrComment()
        }
    }
}

private fun PsiElement.isWhitespaceOrComment(): Boolean = this is PsiWhiteSpace || this is PsiComment


/**
 * Retrieves [KaDanglingFileResolutionMode] calculated by [KaDanglingFileResolutionModeProvider] from the file cache
 * or computes it using [computeMode].
 *
 * The cached value is stored in user data properties of [this] and depends
 * on the modification stamps of [this] and its [copyOrigin].
 * If the sum of these modification stamps is incremented, the previously cached value is recalculated.
 */
private fun KtFile.getOrComputeMode(
    computeMode: () -> KaDanglingFileResolutionMode
): KaDanglingFileResolutionMode {
    return CachedValuesManager.getCachedValue(this) {
        CachedValueProvider.Result.createSingleDependency(
            computeMode(),
            ModificationTracker { modificationStamp + (copyOrigin?.modificationStamp ?: 0) })
    }
}
