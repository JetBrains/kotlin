/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import com.intellij.lang.LighterASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.util.getChildren
import org.jetbrains.kotlin.utils.addToStdlib.butIf
import org.jetbrains.kotlin.utils.addToStdlib.popLast

fun KtSourceElement.getChild(type: IElementType, index: Int = 0, depth: Int = -1, reverse: Boolean = false): KtSourceElement? =
    getChild(setOf(type), index, depth, reverse)

fun KtSourceElement.getChild(types: TokenSet, index: Int = 0, depth: Int = -1, reverse: Boolean = false): KtSourceElement? =
    getChild(types.types.toSet(), index, depth, reverse)

fun KtSourceElement.getChild(types: Set<IElementType>, index: Int = 0, depth: Int = -1, reverse: Boolean = false): KtSourceElement? {
    var idx = index

    forEachChildOfType(types, depth, reverse) {
        if (idx-- == 0) {
            return it
        }
    }

    return null
}

/**
 * Iterates recursively over all children up to the given depth.
 * `processChild` is invoked for each child having a type in the `types` set.
 */
inline fun KtSourceElement.forEachChildOfType(
    types: Set<IElementType>,
    depth: Int = -1,
    reverse: Boolean = false,
    processChild: (KtSourceElement) -> Unit,
) = when (this) {
    is KtPsiSourceElement -> psi.forEachChildOfType(types, depth, reverse) {
        processChild(it.toKtPsiSourceElement())
    }
    is KtLightSourceElement -> lighterASTNode.forEachChildOfType(types, depth, reverse, treeStructure) {
        processChild(it.toKtLightSourceElement(treeStructure))
    }
}

/**
 * See [KtSourceElement.forEachChildOfType]
 */
inline fun PsiElement.forEachChildOfType(
    types: Set<IElementType>,
    depth: Int = -1,
    reverse: Boolean = false,
    processChild: (PsiElement) -> Unit,
) = forEachChildOfType(
    this, types, depth, reverse,
    getElementType = { it.node.elementType },
    getChildren = { it.allChildren.toList() },
    processChild,
)

/**
 * See [KtSourceElement.forEachChildOfType]
 */
inline fun LighterASTNode.forEachChildOfType(
    types: Set<IElementType>,
    depth: Int = -1,
    reverse: Boolean = false,
    treeStructure: FlyweightCapableTreeStructure<LighterASTNode>,
    processChild: (LighterASTNode) -> Unit,
) = forEachChildOfType(
    this, types, depth, reverse,
    getElementType = { it.tokenType },
    getChildren = { it.getChildren(treeStructure) },
    processChild,
)

inline fun <T> forEachChildOfType(
    root: T,
    types: Set<IElementType>,
    depth: Int = -1,
    reverse: Boolean = false,
    getElementType: (T) -> IElementType,
    getChildren: (T) -> List<T>,
    processChild: (T) -> Unit,
) {
    val stack = mutableListOf(root to 0)

    while (stack.isNotEmpty()) {
        val (element, currentDepth) = stack.popLast()

        if (currentDepth != 0 && getElementType(element) in types) {
            processChild(element)
        }

        if (currentDepth == depth) {
            continue
        }

        getChildren(element).butIf(!reverse) { it.asReversed() }.forEach { child ->
            stack += child to (currentDepth + 1)
        }
    }
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

/**
 * Looks for the source element of the last segment
 * of `importedFqName`.
 */
fun FirImport.getLastImportedFqNameSegmentSource(): KtSourceElement? =
    source?.getChild(KtNodeTypes.REFERENCE_EXPRESSION, reverse = true)
