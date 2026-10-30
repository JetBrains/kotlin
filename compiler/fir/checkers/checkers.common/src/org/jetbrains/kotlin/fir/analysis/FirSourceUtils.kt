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
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.syntax.KotlinSyntaxNodeTypeIds
import org.jetbrains.kotlin.util.getChildren
import org.jetbrains.kotlin.utils.addToStdlib.butIf
import org.jetbrains.kotlin.utils.addToStdlib.popLast

fun KtSourceElement.getChild(type: IElementType, index: Int = 0, depth: Int = -1, reverse: Boolean = false): KtSourceElement? =
    getChildMatching(index, depth, reverse) { it == type }

fun KtSourceElement.getChild(types: TokenSet, index: Int = 0, depth: Int = -1, reverse: Boolean = false): KtSourceElement? =
    getChildMatching(index, depth, reverse) { it in types }

private inline fun KtSourceElement.getChildMatching(
    index: Int,
    depth: Int,
    reverse: Boolean,
    isTargetType: (IElementType) -> Boolean,
): KtSourceElement? {
    var idx = index

    forEachChildMatching(depth, reverse, isTargetType) {
        if (idx-- == 0) {
            return it
        }
    }

    return null
}

/**
 * Iterates recursively over all children up to the given [depth] or indefinitely if [depth] is -1.
 * `processChild` is invoked for each child having a type in the `types` set.
 */
inline fun KtSourceElement.forEachChildOfType(
    types: TokenSet,
    depth: Int,
    reverse: Boolean = false,
    processChild: (KtSourceElement) -> Unit,
) {
    forEachChildMatching(depth, reverse, isTargetType = { it in types }, processChild)
}

/**
 * See [KtSourceElement.forEachChildOfType]. `processChild` is invoked for each child whose type satisfies [isTargetType].
 */
@PublishedApi
internal inline fun KtSourceElement.forEachChildMatching(
    depth: Int,
    reverse: Boolean,
    isTargetType: (IElementType) -> Boolean,
    processChild: (KtSourceElement) -> Unit,
) {
    when (this) {
        is KtPsiSourceElement -> {
            forEachChildOfType(
                psi, depth, reverse, isTargetType,
                getElementType = { it.node.elementType },
                getChildren = { it.allChildren.toList() },
            ) {
                processChild(it.toKtPsiSourceElement())
            }
        }
        is KtLightSourceElement -> {
            forEachChildOfType(
                lighterASTNode, depth, reverse, isTargetType,
                getElementType = { it.tokenType },
                getChildren = { it.getChildren(treeStructure) },
            ) {
                processChild(it.toKtLightSourceElement(treeStructure))
            }
        }
    }
}

inline fun <T> forEachChildOfType(
    root: T,
    depth: Int,
    reverse: Boolean = false,
    isTargetType: (IElementType) -> Boolean,
    getElementType: (T) -> IElementType,
    getChildren: (T) -> List<T>,
    processChild: (T) -> Unit,
) {
    val stack = mutableListOf(root to 0)

    while (stack.isNotEmpty()) {
        val [element, currentDepth] = stack.popLast()

        if (currentDepth != 0 && isTargetType(getElementType(element))) {
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

    return segmentSource.takeIf { it.syntaxId == KotlinSyntaxNodeTypeIds.REFERENCE_EXPRESSION }
        ?: segmentSource.getChild(KtNodeTypes.REFERENCE_EXPRESSION, depth = 1, reverse = true)
}

/**
 * Looks for the source element of the last segment
 * of `importedFqName`.
 */
fun FirImport.getLastImportedFqNameSegmentSource(): KtSourceElement? =
    source?.getChild(KtNodeTypes.REFERENCE_EXPRESSION, reverse = true)
