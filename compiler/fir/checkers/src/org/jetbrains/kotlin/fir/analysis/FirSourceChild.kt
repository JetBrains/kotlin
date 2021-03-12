/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.fir.*

fun FirSourceElement.getChild(type: IElementType, index: Int = 0, depth: Int = -1): FirSourceElement? {
    return getChild(setOf(type), index, depth)
}

fun FirSourceElement.getChild(types: TokenSet, index: Int = 0, depth: Int = -1): FirSourceElement? {
    return getChild(types.types.toSet(), index, depth)
}

fun FirSourceElement.getChild(types: Set<IElementType>, index: Int = 0, depth: Int = -1): FirSourceElement? {
    return when (this) {
        is FirPsiSourceElement<*> -> {
            getChild(types, index, depth)
        }
        is FirLightSourceElement -> {
            getChild(types, index, depth)
        }
        else -> null
    }
}

private fun FirPsiSourceElement<*>.getChild(types: Set<IElementType>, index: Int, depth: Int): FirSourceElement? {
    val visitor = PsiElementFinderByType(types, index, depth)
    return visitor.find(psi)?.toFirPsiSourceElement()
}

private fun FirLightSourceElement.getChild(types: Set<IElementType>, index: Int, depth: Int): FirSourceElement? {
    val visitor = LighterTreeElementFinderByType(treeStructure, types, index, depth)
    val node = lighterASTNode
    val childNode = visitor.find(node) ?: return null
    // org.jetbrains.kotlin.fir.lightTree.converter.DeclarationsConverter creates subtree for BLOCKs and use the nodes from these subtrees
    // as the source.
    val subtreeOffset = this.startOffset - node.startOffset
    return childNode.toFirLightSourceElement(
        treeStructure,
        startOffset = subtreeOffset + childNode.startOffset,
        endOffset = subtreeOffset + childNode.endOffset
    )
}
