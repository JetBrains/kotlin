/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.lang.LighterASTNode
import com.intellij.lang.TreeBackedLighterAST
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure

// NB: in certain situations, psi.node could be null (see e.g. KT-44152)
// Potentially exceptions can be provoked by elementType / lighterASTNode
sealed class KtPsiSourceElement(val psi: PsiElement) : KtSourceElement() {
    override val elementType: IElementType?
        get() = psi.node?.elementType

    override val startOffset: Int
        get() = psi.textRange.startOffset

    override val endOffset: Int
        get() = psi.textRange.endOffset

    override val lighterASTNode: LighterASTNode by lazy(LazyThreadSafetyMode.PUBLICATION) {
        TreeBackedLighterAST.wrap(psi.node)
    }

    override val treeStructure: FlyweightCapableTreeStructure<LighterASTNode> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        WrappedTreeStructure(psi.containingFile)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KtPsiSourceElement

        if (psi != other.psi) return false

        return true
    }

    override fun hashCode(): Int {
        return psi.hashCode()
    }
}

internal class KtRealPsiSourceElement(psi: PsiElement) : KtPsiSourceElement(psi) {
    override val kind: KtSourceElementKind get() = KtRealSourceElementKind
}

internal class KtFakeSourceElement(psi: PsiElement, override val kind: KtFakeSourceElementKind) : KtPsiSourceElement(psi) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as KtFakeSourceElement

        if (kind != other.kind) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + kind.hashCode()
        return result
    }
}

fun PsiElement.toKtPsiSourceElement(kind: KtSourceElementKind = KtRealSourceElementKind): KtPsiSourceElement = when (kind) {
    is KtRealSourceElementKind -> KtRealPsiSourceElement(this)
    is KtFakeSourceElementKind -> KtFakeSourceElement(this, kind)
}

