/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.lang.LighterASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure

sealed class FirSourceElement {
    abstract val elementType: IElementType
    abstract val startOffset: Int
    abstract val endOffset: Int
}

class FirPsiSourceElement<P : PsiElement>(val psi: P) : FirSourceElement() {
    override val elementType: IElementType
        get() = psi.node.elementType

    override val startOffset: Int
        get() = psi.textRange.startOffset

    override val endOffset: Int
        get() = psi.textRange.endOffset
}

class FirLightSourceElement(
    val element: LighterASTNode,
    override val startOffset: Int,
    override val endOffset: Int,
    val tree: FlyweightCapableTreeStructure<LighterASTNode>
) : FirSourceElement() {
    override val elementType: IElementType
        get() = element.tokenType
}

val FirSourceElement?.psi: PsiElement? get() = (this as? FirPsiSourceElement<*>)?.psi

val FirElement.psi: PsiElement? get() = (source as? FirPsiSourceElement<*>)?.psi

@Suppress("NOTHING_TO_INLINE")
inline fun PsiElement.toFirPsiSourceElement(): FirPsiSourceElement<*> = FirPsiSourceElement(this)

@Suppress("NOTHING_TO_INLINE")
inline fun LighterASTNode.toFirLightSourceElement(
    startOffset: Int, endOffset: Int,
    tree: FlyweightCapableTreeStructure<LighterASTNode>
): FirLightSourceElement = FirLightSourceElement(this, startOffset, endOffset, tree)

val FirSourceElement?.lightNode: LighterASTNode? get() = (this as? FirLightSourceElement)?.element
