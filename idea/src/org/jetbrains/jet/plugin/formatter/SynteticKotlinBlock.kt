/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.formatter

import com.intellij.formatting.Block
import com.intellij.formatting.Alignment
import com.intellij.formatting.Indent
import com.intellij.formatting.Wrap
import com.intellij.openapi.util.TextRange
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.Spacing
import com.intellij.lang.ASTNode
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.formatting.ASTBlock
import java.util.ArrayList

public class SyntheticKotlinBlock(
        private val _node: ASTNode,
        private val _subBlocks: MutableList<Block>,
        private val _alignment: Alignment?,
        private val _indent: Indent?,
        private val _wrap: Wrap?,
        private val spacingBuilder: KotlinSpacingBuilder) : ASTBlock {

    private val _textRange = TextRange(
            _subBlocks.first().getTextRange().getStartOffset(),
            _subBlocks.last().getTextRange().getEndOffset())

    override fun getTextRange(): TextRange = _textRange
    override fun getSubBlocks() = _subBlocks
    override fun getWrap() = _wrap
    override fun getIndent() = _indent
    override fun getAlignment() = _alignment
    override fun getChildAttributes(newChildIndex: Int) = ChildAttributes(getIndent(), null)
    override fun isIncomplete() = getSubBlocks().last().isIncomplete()
    override fun isLeaf() = false
    override fun getNode() = _node
    override fun getSpacing(child1: Block?, child2: Block): Spacing? =
            spacingBuilder.getSpacing(KotlinSpacingBuilder.SpacingNodeBlock(_node.getTreeParent()!!), child1, child2)


    override fun toString(): String {
        var child = _subBlocks.first()
        var treeNode: ASTNode? = null
        while (treeNode == null) when (child) {
            is AbstractBlock -> {
                treeNode = (child as AbstractBlock).getNode()
            }
            is SyntheticKotlinBlock -> {
                child = (child as SyntheticKotlinBlock).getSubBlocks().first()
            }
            else -> break
        }

        val textRange = getTextRange()
        if (treeNode != null) {
            val psi = treeNode!!.getPsi()
            if (psi != null) {
                val file = psi.getContainingFile()
                if (file != null) {
                    return file.getText()!!.subSequence(textRange.getStartOffset(), textRange.getEndOffset()).toString() + " " + textRange
                }
            }
        }

        return javaClass.getName() + ": " + textRange
    }
}
