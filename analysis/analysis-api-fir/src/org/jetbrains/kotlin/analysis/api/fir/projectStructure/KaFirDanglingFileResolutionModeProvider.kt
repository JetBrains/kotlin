/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.projectStructure

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.ASTStructure
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import com.intellij.util.ThreeState
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
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.psi.psiUtil.textRangeWithoutComments

internal class KaFirDanglingFileResolutionModeProvider(private val project: Project) : KaDanglingFileResolutionModeProvider {
    @OptIn(LLFirInternals::class)
    override fun calculateMode(file: KtFile): KaDanglingFileResolutionMode {
        val originalFile = file.copyOrigin as? KtFile ?: return KaDanglingFileResolutionMode.IGNORE_SELF
        val originalNode = ASTStructure(originalFile.node)
        val copyNode = ASTStructure(file.node)

        val modificationService = LLFirDeclarationModificationService.getInstance(project)
        val consumer = Consumer(modificationService)
        DiffTree.diff(
            /* oldTree = */ originalNode,
            /* newTree = */ copyNode,
            /* comparator = */ NodeComparator(),
            /* consumer = */ consumer,
            /* oldText = */ originalFile.text
        )
        return consumer.mode
    }

    private class NodeComparator : ShallowNodeComparator<ASTNode, ASTNode> {
        private fun PsiElement.getTextWithoutComments(): String {
            val startOffset = startOffset
            val startWithoutComments = textRangeWithoutComments.startOffset - startOffset
            val endOffset = this@getTextWithoutComments.endOffset - startOffset

            return text.substring(startWithoutComments, endOffset)
        }

        override fun deepEqual(oldNode: ASTNode, newNode: ASTNode): ThreeState {
            if (oldNode.elementType != newNode.elementType) return ThreeState.NO
            if (oldNode.psi.getTextWithoutComments() == newNode.psi.getTextWithoutComments()) return ThreeState.YES
            return ThreeState.UNSURE
        }

        override fun typesEqual(oldNode: ASTNode, newNode: ASTNode): Boolean {
            return oldNode.elementType == newNode.elementType
        }

        override fun hashCodesEqual(oldNode: ASTNode, newNode: ASTNode): Boolean {
            return oldNode.hashCode() == newNode.hashCode()
        }
    }

    @OptIn(LLFirInternals::class)
    private class Consumer(private val modificationService: LLFirDeclarationModificationService) : DiffTreeChangeBuilder<ASTNode, ASTNode> {
        var mode: KaDanglingFileResolutionMode = KaDanglingFileResolutionMode.IGNORE_SELF

        override fun nodeReplaced(oldChild: ASTNode, newChild: ASTNode) {
            if (mode == KaDanglingFileResolutionMode.PREFER_SELF) {
                return
            }

            val oldNodePsi = oldChild.psi
            val newNodePsi = newChild.psi

            /**
             * Unfortunately, [LLFirDeclarationModificationService] doesn't directly support replacement as a single modification.
             * So we need to run two modifications: removal and addition.
             */
            val changeTypeForDeletion =
                calculateChangeType(oldNodePsi.parent, KaElementModificationType.ElementRemoved(oldNodePsi))
            if (changeTypeForDeletion is KaSourceModificationLocality.OutOfBlock) {
                if (!oldNodePsi.isWhitespaceOrComment() || oldNodePsi.isSeparatingWhitespaceOrComment() && !newNodePsi.isWhitespaceOrComment()) {
                    /**
                     * There might be cases when one whitespace / comment node is replaced with another whitespace / comment node.
                     * ```kotlin
                     * interface A -> interface/* my comment */A
                     * ```
                     * The initial deletion here is considered OOBM. However, the replacement node is a comment that still separates other nodes.
                     * That's why we avoid changing the resolution mode in such cases.
                     */
                    mode = KaDanglingFileResolutionMode.PREFER_SELF
                    return
                }
            }


            val changeTypeForInsertion =
                calculateChangeType(newNodePsi, KaElementModificationType.ElementAdded)
            if (changeTypeForInsertion is KaSourceModificationLocality.OutOfBlock) {
                mode = KaDanglingFileResolutionMode.PREFER_SELF
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
                if (oldNodePsi.isWhitespaceOrComment() && !oldNodePsi.isSeparatingWhitespaceOrComment()) {
                    /**
                     * If the deleted node is whitespace / comment that didn't separate other nodes,
                     * we can be sure that the deletion doesn't affect the resolution.
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

        private fun PsiElement.isWhitespaceOrComment(): Boolean = this is PsiWhiteSpace || this is PsiComment
    }
}