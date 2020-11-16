/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
    val visitor = LighterTreeElementFinderByType(tree, types, index, depth)

    return visitor.find(lighterASTNode)?.let { it.toFirLightSourceElement(it.startOffset, it.endOffset, tree) }
}