/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.toKtLightSourceElement
import org.jetbrains.kotlin.toKtPsiSourceElement
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
 * Iterates recursively over all children up to the given [depth] or indefinitely if [depth] is -1.
 * `processChild` is invoked for each child having a type in the `types` set.
 */
inline fun KtSourceElement.forEachChildOfType(
    types: Set<IElementType>,
    depth: Int,
    reverse: Boolean = false,
    processChild: (KtSourceElement) -> Unit,
) {
    when (this) {
        is KtPsiSourceElement ->
            forEachChildOfType(
                psi, types, depth, reverse,
                getElementType = { it.node.elementType },
                getChildren = { it.allChildren.toList() },
            ) {
                processChild(it.toKtPsiSourceElement())
            }

        is KtLightSourceElement ->
            forEachChildOfType(
                lighterASTNode, types, depth, reverse,
                getElementType = { it.tokenType },
                getChildren = { it.getChildren(treeStructure) },
            ) {
                processChild(it.toKtLightSourceElement(treeStructure))
            }
    }
}

@PublishedApi
internal inline fun <T> forEachChildOfType(
    root: T,
    types: Set<IElementType>,
    depth: Int,
    reverse: Boolean = false,
    getElementType: (T) -> IElementType,
    getChildren: (T) -> List<T>,
    processChild: (T) -> Unit,
) {
    val stack = mutableListOf(root to 0)

    while (stack.isNotEmpty()) {
        val [element, currentDepth] = stack.popLast()

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
