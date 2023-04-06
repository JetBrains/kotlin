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
abstract class KtPsiSourceElement : KtSourceElement() {
    abstract val psi: PsiElement

    override val elementType: IElementType?
        get() = psi.node?.elementType

    override val startOffset: Int
        get() = psi.textRange.startOffset

    override val endOffset: Int
        get() = psi.textRange.endOffset

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

private sealed class KtFixedPsiSourceElement(override val psi: PsiElement) : KtPsiSourceElement() {
    override val lighterASTNode: LighterASTNode by lazy(LazyThreadSafetyMode.PUBLICATION) {
        TreeBackedLighterAST.wrap(psi.node)
    }

    override val treeStructure: FlyweightCapableTreeStructure<LighterASTNode> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        WrappedTreeStructure(psi.containingFile)
    }

    class KtRealPsiSourceElement(psi: PsiElement) : KtFixedPsiSourceElement(psi) {
        override val kind: KtSourceElementKind get() = KtRealSourceElementKind

        override fun fakeElement(newKind: KtFakeSourceElementKind): KtSourceElement {
            return KtFakeSourceElement(psi, newKind)
        }

        override fun realElement(): KtSourceElement {
            return this
        }
    }

    class KtFakeSourceElement(psi: PsiElement, override val kind: KtFakeSourceElementKind) : KtFixedPsiSourceElement(psi) {
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

        override fun fakeElement(newKind: KtFakeSourceElementKind): KtSourceElement {
            if (kind == newKind) return this
            return KtFakeSourceElement(psi, newKind)
        }

        override fun realElement(): KtSourceElement {
            return KtRealPsiSourceElement(psi)
        }
    }
}


fun PsiElement.toKtPsiSourceElementWithFixedPsi(kind: KtSourceElementKind = KtRealSourceElementKind): KtPsiSourceElement = when (kind) {
    is KtRealSourceElementKind -> KtFixedPsiSourceElement.KtRealPsiSourceElement(this)
    is KtFakeSourceElementKind -> KtFixedPsiSourceElement.KtFakeSourceElement(this, kind)
}

