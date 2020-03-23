/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

sealed class FirSourceElement {
    abstract val elementType: IElementType
    abstract val startOffset: Int
    abstract val endOffset: Int
}

class FirPsiSourceElement(val psi: PsiElement) : FirSourceElement() {
    override val elementType: IElementType
        get() = psi.node.elementType

    override val startOffset: Int
        get() = psi.startOffset

    override val endOffset: Int
        get() = psi.endOffset
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

val FirSourceElement?.psi: PsiElement? get() = (this as? FirPsiSourceElement)?.psi

val FirElement.psi: PsiElement? get() = (source as? FirPsiSourceElement)?.psi

@Suppress("NOTHING_TO_INLINE")
inline fun PsiElement.toFirSourceElement(): FirPsiSourceElement = FirPsiSourceElement(this)

val FirSourceElement?.lightNode: LighterASTNode? get() = (this as? FirLightSourceElement)?.element

fun FirSourceElement?.getModifierList(): FirModifierList? {
    return when (this) {
        null -> null
        is FirPsiSourceElement -> (psi as? KtModifierListOwner)?.modifierList?.let { FirPsiModifierList(it) }
        is FirLightSourceElement -> {
            val kidsRef = Ref<Array<LighterASTNode?>>()
            tree.getChildren(element, kidsRef)
            val modifierListNode = kidsRef.get().find { it?.tokenType == KtNodeTypes.MODIFIER_LIST } ?: return null
            FirLightModifierList(modifierListNode, tree)
        }
    }
}
