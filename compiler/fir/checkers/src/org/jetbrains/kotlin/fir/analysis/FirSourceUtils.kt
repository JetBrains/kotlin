/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.fir.declarations.FirImport

fun KtSourceElement.getChild(type: IElementType, index: Int = 0, depth: Int = -1, reverse: Boolean = false): KtSourceElement? {
    return getChild(setOf(type), index, depth, reverse)
}

fun KtSourceElement.getChild(types: TokenSet, index: Int = 0, depth: Int = -1, reverse: Boolean = false): KtSourceElement? {
    return getChild(types.types.toSet(), index, depth, reverse)
}

fun KtSourceElement.getChild(types: Set<IElementType>, index: Int = 0, depth: Int = -1, reverse: Boolean = false): KtSourceElement? {
    return when (this) {
        is KtPsiSourceElement -> {
            getChild(types, index, depth, reverse)
        }
        is KtLightSourceElement -> {
            getChild(types, index, depth, reverse)
        }
        else -> null
    }
}

private fun KtPsiSourceElement.getChild(types: Set<IElementType>, index: Int, depth: Int, reverse: Boolean): KtSourceElement? {
    val visitor = PsiElementFinderByType(types, index, depth, reverse)
    return visitor.find(psi)?.toKtPsiSourceElement()
}

private fun KtLightSourceElement.getChild(types: Set<IElementType>, index: Int, depth: Int, reverse: Boolean): KtSourceElement? {
    val visitor = LighterTreeElementFinderByType(treeStructure, types, index, depth, reverse)
    val childNode = visitor.find(lighterASTNode) ?: return null
    return buildChildSourceElement(childNode)
}

/**
 * Keeps 'padding' of parent node in child node
 */
internal fun KtLightSourceElement.buildChildSourceElement(childNode: LighterASTNode): KtLightSourceElement {
    val offsetDelta = startOffset - lighterASTNode.startOffset
    return childNode.toKtLightSourceElement(
        treeStructure,
        startOffset = childNode.startOffset + offsetDelta,
        endOffset = childNode.endOffset + offsetDelta
    )
}

private val IMPORT_PARENT_TOKEN_TYPES = TokenSet.create(KtNodeTypes.DOT_QUALIFIED_EXPRESSION, KtNodeTypes.REFERENCE_EXPRESSION)

/**
 * Returns a source element for the import segment that is [indexFromLast]th from last.
 */
fun FirImport.getSourceForImportSegment(indexFromLast: Int): KtSourceElement? {
    var segmentSource: KtSourceElement = source ?: return null

    repeat(indexFromLast + 1) {
        segmentSource = segmentSource.getChild(IMPORT_PARENT_TOKEN_TYPES, depth = 1) ?: return null
    }

    return segmentSource.takeIf { it.elementType == KtNodeTypes.REFERENCE_EXPRESSION }
        ?: segmentSource.getChild(KtNodeTypes.REFERENCE_EXPRESSION, depth = 1, reverse = true)
}
