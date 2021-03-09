/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import com.intellij.lang.LighterASTNode
import com.intellij.psi.PsiElement
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
    return visitor.find(lighterASTNode)?.let { withNode(it) }
}

fun FirSourceElement.getParent(type: IElementType, includingSelf: Boolean = false): FirSourceElement? {
    return getParent(setOf(type), includingSelf)
}

fun FirSourceElement.getParent(types: TokenSet, includingSelf: Boolean = false): FirSourceElement? {
    return getParent(types.types.toSet(), includingSelf)
}

fun FirSourceElement.getParent(types: Set<IElementType>, includingSelf: Boolean = false): FirSourceElement? {
    return when (this) {
        is FirPsiSourceElement<*> -> {
            getParent(types, includingSelf)
        }
        is FirLightSourceElement -> {
            getParent(types, includingSelf)
        }
        else -> null
    }
}

private fun FirPsiSourceElement<*>.getParent(types: Set<IElementType>, includingSelf: Boolean): FirSourceElement? {
    var parent: PsiElement? = if (includingSelf) psi else psi.parent
    while (parent != null && parent.node.elementType !in types) {
        parent = parent.parent
    }
    return parent?.toFirPsiSourceElement()
}

private fun FirLightSourceElement.getParent(types: Set<IElementType>, includingSelf: Boolean): FirSourceElement? {
    var parent: LighterASTNode? = if (includingSelf) lighterASTNode else treeStructure.getParent(lighterASTNode)
    while (parent != null && parent.tokenType !in types) {
        parent = treeStructure.getParent(parent)
    }
    return parent?.let { withNode(it) }
}

private fun FirLightSourceElement.withNode(newNode: LighterASTNode): FirLightSourceElement {
    // It seems sometimes the `startOffset` from a `LighterASTNode` could count from some partial sub tree of the file. If this is the case,
    // we need to compute the delta between the corresponding position in this partial tree and the file start. The latter can be retrieved
    // from `FirLightSourceElement`.
    val startDelta = this.startOffset - lighterASTNode.startOffset
    val endDelta = this.endOffset - lighterASTNode.endOffset
    return newNode.toFirLightSourceElement(
        treeStructure,
        startOffset = startDelta + newNode.startOffset,
        endOffset = endDelta + newNode.endOffset
    )
}
