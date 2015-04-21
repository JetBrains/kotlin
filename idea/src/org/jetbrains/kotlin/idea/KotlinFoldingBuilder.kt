/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea

import com.intellij.codeInsight.folding.JavaCodeFoldingSettings
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetImportDirective
import org.jetbrains.kotlin.psi.JetImportList

import java.util.ArrayList

public class KotlinFoldingBuilder : FoldingBuilderEx(), DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        if (root !is JetFile) {
            return FoldingDescriptor.EMPTY
        }
        val descriptors = ArrayList<FoldingDescriptor>()

        val imports = root.getImportDirectives()
        if (imports.size() > 1) {
            val importKeyword = imports.get(0).getFirstChild()
            val startOffset = importKeyword.getTextRange().getEndOffset() + 1

            val importList = root.getImportList()
            if (importList != null) {
                val endOffset = importList.getTextRange().getEndOffset()

                val range = TextRange(startOffset, endOffset)
                descriptors.add(FoldingDescriptor(importList, range))
            }
        }

        appendDescriptors(root.getNode(), document, descriptors)
        return descriptors.copyToArray()
    }

    private fun appendDescriptors(node: ASTNode, document: Document, descriptors: MutableList<FoldingDescriptor>) {
        val textRange = node.getTextRange()
        val type = node.getElementType()
        if ((type == JetNodeTypes.BLOCK || type == JetNodeTypes.CLASS_BODY || type == JetTokens.BLOCK_COMMENT || type == KDocTokens.KDOC) &&
                !isOneLine(textRange, document)) {
            descriptors.add(FoldingDescriptor(node, textRange))
        }
        var child: ASTNode? = node.getFirstChildNode()
        while (child != null) {
            appendDescriptors(child, document, descriptors)
            child = child.getTreeNext()
        }
    }

    private fun isOneLine(textRange: TextRange, document: Document) =
        document.getLineNumber(textRange.getStartOffset()) == document.getLineNumber(textRange.getEndOffset())

    override fun getPlaceholderText(node: ASTNode): String? {
        if (node.getElementType() == JetTokens.BLOCK_COMMENT) {
            return "/.../"
        }
        if (node.getElementType() == KDocTokens.KDOC) {
            return "/**...*/"
        }
        if (node.getPsi() is JetImportList) {
            return "..."
        }
        return "{...}"
    }

    override fun isCollapsedByDefault(astNode: ASTNode): Boolean {
        val settings = JavaCodeFoldingSettings.getInstance()

        if (astNode.getPsi() is JetImportList) {
            return settings.isCollapseImports()
        }

        val type = astNode.getElementType()
        if (type == JetTokens.BLOCK_COMMENT || type == KDocTokens.KDOC) {
            if (isFirstElementInFile(astNode.getPsi())) {
                return settings.isCollapseFileHeader()
            }
        }

        return false
    }

    private fun isFirstElementInFile(element: PsiElement): Boolean {
        val parent = element.getParent()
        if (parent is JetFile) {
            var firstChild = parent.getFirstChild()
            if (firstChild is PsiWhiteSpace) {
                firstChild = firstChild.getNextSibling()
            }

            return element == firstChild
        }

        return false
    }
}
