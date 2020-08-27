/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.fir.*

fun FirSourceElement.getChildren(type: IElementType, index: Int = 0, depth: Int = -1): FirSourceElement? {
    return getChildren(setOf(type), index, depth)
}

fun FirSourceElement.getChildren(types: TokenSet, index: Int = 0, depth: Int = -1): FirSourceElement? {
    return getChildren(types.types.toSet(), index, depth)
}

fun FirSourceElement.getChildren(types: Set<IElementType>, index: Int = 0, depth: Int = -1): FirSourceElement? {
    return when (this) {
        is FirPsiSourceElement<*> -> {
            getChildren(types, index, depth)
        }
        is FirLightSourceElement -> {
            getChildren(types, index, depth)
        }
        else -> null
    }
}

private fun FirPsiSourceElement<*>.getChildren(types: Set<IElementType>, index: Int, depth: Int): FirSourceElement? {
    val visitor = PsiElementFinderByType(types, index, depth)
    return visitor.find(psi)?.toFirPsiSourceElement()
}

private fun FirLightSourceElement.getChildren(types: Set<IElementType>, index: Int, depth: Int): FirSourceElement? {
    val visitor = LighterTreeElementFinderByType(tree, types, index, depth)

    return visitor.find(lightNode)?.let { it.toFirLightSourceElement(it.startOffset, it.endOffset, tree) }
}