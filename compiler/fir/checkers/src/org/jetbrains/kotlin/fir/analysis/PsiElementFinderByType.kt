/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.psi.psiUtil.allChildren

class PsiElementFinderByType(
    private val types: Collection<IElementType>,
    private var index: Int,
    private val depth: Int,
    private val reverse: Boolean,
) {
    fun find(root: PsiElement): PsiElement? {
        return visitElement(root, 0)
    }

    private fun visitElement(element: PsiElement, currentDepth: Int): PsiElement? {
        if (currentDepth != 0) {
            if (element.node.elementType in types) {
                if (index == 0) {
                    return element
                }
                index--
            }
        }

        if (currentDepth == depth) return null

        val children = if (reverse) element.allChildren.toList().asReversed().iterator() else element.allChildren.iterator()
        for (child in children) {
            val result = visitElement(child, currentDepth + 1)
            if (result != null) return result
        }

        return null
    }

}
