/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.toFirLightSourceElement
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.lexer.KtTokens

private typealias Tree = FlyweightCapableTreeStructure<LighterASTNode>

private class LightTreeErrorsCollector(private val tree: Tree) {

    private fun LighterASTNode.getChildrenAsArray(): Array<out LighterASTNode?> {
        val kidsRef = Ref<Array<LighterASTNode?>>()
        return if (tree.getChildren(this, kidsRef) > 0) kidsRef.get() else emptyArray()
    }

    private inline fun LighterASTNode.forEachChildren(f: (LighterASTNode) -> Unit) {
        val kidsArray = this.getChildrenAsArray()
        for (kid in kidsArray) {
            if (kid == null) break
            val tokenType = kid.tokenType
            if (KtTokens.COMMENTS.contains(tokenType) || tokenType == KtTokens.WHITE_SPACE || tokenType == KtTokens.SEMICOLON) continue
            f(kid)
        }
    }

    fun collectErrorNodes(node: LighterASTNode, acc: MutableList<LighterASTNode> = mutableListOf()): List<LighterASTNode> {
        if (node.tokenType == TokenType.ERROR_ELEMENT) {
            acc.add(node)
        } else {
            node.forEachChildren { child ->
                collectErrorNodes(child, acc)
            }
        }
        return acc
    }

}

private data class OffsetTree(val tree: Tree, val offset: Int)

private object FirTreesExtractVisitor : FirVisitor<Unit, Pair<Tree?, MutableList<OffsetTree>>>() {
    override fun visitElement(element: FirElement, data: Pair<Tree?, MutableList<OffsetTree>>) {
        val source = element.source ?: return
        val currentTree = source.treeStructure
        val (lastTree, trees) = data
        val newData = if (lastTree !== currentTree) {
            val newTreeWithOffset = OffsetTree(currentTree, element.source?.startOffset ?: 0)
            trees.add(newTreeWithOffset)
            currentTree to trees
        } else {
            data
        }
        element.acceptChildren(this, newData)
    }
}

internal fun collectLightTreeSyntaxErrors(file: FirFile): List<FirLightSourceElement> {
    val trees = mutableListOf<OffsetTree>()
    file.accept(FirTreesExtractVisitor, null to trees)
    return trees.flatMap { (tree, offset) ->
        LightTreeErrorsCollector(tree).collectErrorNodes(tree.root).map {
            it.toFirLightSourceElement(tree, startOffset = it.startOffset + offset, endOffset = it.endOffset + offset)
        }
    }
}
