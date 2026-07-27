/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(SuspiciousFakeSourceCheck::class)

package org.jetbrains.kotlin

import com.intellij.lang.LighterASTNode
import com.intellij.lang.TreeBackedLighterAST
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.diagnostics.AbstractSourceElementPositioningStrategy
import org.jetbrains.kotlin.diagnostics.DiagnosticBaseContext
import org.jetbrains.kotlin.diagnostics.InternalDiagnosticFactoryMethod
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory4
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithParameters1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithParameters2
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithParameters3
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithParameters4
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnosticWithParameters1
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnosticWithParameters2
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnosticWithParameters3
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnosticWithParameters4
import org.jetbrains.kotlin.diagnostics.KtPsiSimpleDiagnostic
import org.jetbrains.kotlin.diagnostics.KtSimpleDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.psi.psiUtil.UNWRAPPABLE_TOKEN_TYPES
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentLhsIfUnwrappable
import org.jetbrains.kotlin.psi.psiUtil.getExplicitReceiverOfDotQualified
import org.jetbrains.kotlin.resolve.source.getAssignmentLhsIfUnwrappable
import org.jetbrains.kotlin.resolve.source.getExplicitReceiverOfDotQualified
import org.jetbrains.kotlin.utils.getElementTextWithContext
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

// NB: in certain situations, psi.node could be null (see e.g. KT-44152)
// Potentially exceptions can be provoked by elementType / lighterASTNode
sealed class KtPsiSourceElement(val psi: PsiElement) : KtSourceElement() {
    companion object {
        @JvmStatic
        private val lighterASTNodeUpdater = AtomicReferenceFieldUpdater.newUpdater(
            KtPsiSourceElement::class.java,
            LighterASTNode::class.java,
            "_lighterASTNode"
        )

        @JvmStatic
        private val treeStructureNodeUpdater = AtomicReferenceFieldUpdater.newUpdater(
            KtPsiSourceElement::class.java,
            FlyweightCapableTreeStructure::class.java,
            "_treeStructure"
        )
    }

    override val elementType: IElementType?
        get() = psi.node?.elementType

    override val startOffset: Int
        get() = psi.textRange.startOffset

    override val endOffset: Int
        get() = psi.textRange.endOffset

    @Volatile
    private var _lighterASTNode: LighterASTNode? = null
    final override val lighterASTNode: LighterASTNode
        get() {
            _lighterASTNode?.let { return it }
            lighterASTNodeUpdater.compareAndSet(
                /* obj = */ this,
                /* expect = */ null,
                /* update = */ TreeBackedLighterAST.wrap(psi.node)
            )
            return _lighterASTNode!!
        }

    @Volatile
    private var _treeStructure: FlyweightCapableTreeStructure<LighterASTNode>? = null
    final override val treeStructure: FlyweightCapableTreeStructure<LighterASTNode>
        get() {
            _treeStructure?.let { return it }
            treeStructureNodeUpdater.compareAndSet(
                /* obj = */ this,
                /* expect = */ null,
                /* update = */ WrappedTreeStructure(psi.containingFile)
            )
            return _treeStructure!!
        }

    override fun getElementTextInContextForDebug(): String {
        return getElementTextWithContext(psi)
    }

    internal class WrappedTreeStructure(file: PsiFile) : FlyweightCapableTreeStructure<LighterASTNode> {
        private val lighterAST = TreeBackedLighterAST(file.node)

        fun unwrap(node: LighterASTNode) = lighterAST.unwrap(node)

        override fun toString(node: LighterASTNode): CharSequence = unwrap(node).text

        override fun getRoot(): LighterASTNode = lighterAST.root

        override fun getParent(node: LighterASTNode): LighterASTNode? =
            unwrap(node).psi.parent?.node?.let { TreeBackedLighterAST.wrap(it) }

        override fun getChildren(node: LighterASTNode, nodesRef: Ref<Array<LighterASTNode>>): Int {
            val psi = unwrap(node).psi
            val children = mutableListOf<PsiElement>()
            var child = psi.firstChild
            while (child != null) {
                children += child
                child = child.nextSibling
            }
            if (children.isEmpty()) {
                nodesRef.set(LighterASTNode.EMPTY_ARRAY)
            } else {
                nodesRef.set(children.map { TreeBackedLighterAST.wrap(it.node) }.toTypedArray())
            }
            return children.size
        }

        override fun disposeChildren(p0: Array<out LighterASTNode>?, p1: Int) {
        }

        override fun getStartOffset(node: LighterASTNode): Int {
            return getStartOffset(unwrap(node).psi)
        }

        private fun getStartOffset(element: PsiElement): Int {
            var child = element.firstChild
            if (child != null) {
                while (child is PsiComment || child is PsiWhiteSpace) {
                    child = child.nextSibling
                }
                if (child != null) {
                    return getStartOffset(child)
                }
            }
            return element.textRange.startOffset
        }

        override fun getEndOffset(node: LighterASTNode): Int {
            return getEndOffset(unwrap(node).psi)
        }

        private fun getEndOffset(element: PsiElement): Int {
            var child = element.lastChild
            if (child != null) {
                while (child is PsiComment || child is PsiWhiteSpace) {
                    child = child.prevSibling
                }
                if (child != null) {
                    return getEndOffset(child)
                }
            }
            return element.textRange.endOffset
        }
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

    override fun toString(): String = buildString {
        append(this@KtPsiSourceElement::class.simpleName)
        append('(')
        append(psi::class.simpleName)
        if (kind is KtFakeSourceElementKind) {
            append(", ").append(kind)
        }
        append(", ").append(startOffset).append("..").append(endOffset)
        append(')')
    }

    override fun fakeElement(newKind: KtFakeSourceElementKind, offsetStrategy: KtSourceElementOffsetStrategy): KtSourceElement {
        if (kind == newKind) return this
        return when (offsetStrategy) {
            is KtSourceElementOffsetStrategy.Default -> KtFakePsiSourceElement(psi, newKind)
            is KtSourceElementOffsetStrategy.Custom -> KtFakePsiSourceElementWithCustomOffsetStrategy(psi, newKind, offsetStrategy)
        }
    }

    override fun realElement(): KtSourceElement {
        return KtRealPsiSourceElement(psi)
    }

    override val text: CharSequence
        get() = psi.text

    @InternalDiagnosticFactoryMethod
    override fun createDiagnostic0(
        severity: Severity,
        factory: KtDiagnosticFactory0,
        positioningStrategy: AbstractSourceElementPositioningStrategy,
        context: DiagnosticBaseContext
    ): KtSimpleDiagnostic {
        return KtPsiSimpleDiagnostic(
            this,
            severity,
            factory,
            positioningStrategy,
            context,
        )
    }

    @InternalDiagnosticFactoryMethod
    override fun <A> createDiagnostic1(
        severity: Severity,
        factory: KtDiagnosticFactory1<A>,
        a: A,
        positioningStrategy: AbstractSourceElementPositioningStrategy,
        context: DiagnosticBaseContext
    ): KtDiagnosticWithParameters1<A> {
        return KtPsiDiagnosticWithParameters1(
            this,
            a,
            severity,
            factory,
            positioningStrategy,
            context,
        )
    }

    @InternalDiagnosticFactoryMethod
    override fun <A, B> createDiagnostic2(
        severity: Severity,
        factory: KtDiagnosticFactory2<A, B>,
        a: A,
        b: B,
        positioningStrategy: AbstractSourceElementPositioningStrategy,
        context: DiagnosticBaseContext
    ): KtDiagnosticWithParameters2<A, B> {
        return KtPsiDiagnosticWithParameters2(
            this,
            a,
            b,
            severity,
            factory,
            positioningStrategy,
            context,
        )
    }

    @InternalDiagnosticFactoryMethod
    override fun <A, B, C> createDiagnostic3(
        severity: Severity,
        factory: KtDiagnosticFactory3<A, B, C>,
        a: A,
        b: B,
        c: C,
        positioningStrategy: AbstractSourceElementPositioningStrategy,
        context: DiagnosticBaseContext
    ): KtDiagnosticWithParameters3<A, B, C> {
        return KtPsiDiagnosticWithParameters3(
            this,
            a,
            b,
            c,
            severity,
            factory,
            positioningStrategy,
            context,
        )
    }

    @InternalDiagnosticFactoryMethod
    override fun <A, B, C, D> createDiagnostic4(
        severity: Severity,
        factory: KtDiagnosticFactory4<A, B, C, D>,
        a: A,
        b: B,
        c: C,
        d: D,
        positioningStrategy: AbstractSourceElementPositioningStrategy,
        context: DiagnosticBaseContext,
    ): KtDiagnosticWithParameters4<A, B, C, D> {
        return KtPsiDiagnosticWithParameters4(
            this,
            a,
            b,
            c,
            d,
            severity,
            factory,
            positioningStrategy,
            context,
        )
    }
}

class KtRealPsiSourceElement(psi: PsiElement) : KtPsiSourceElement(psi) {
    override val kind: KtSourceElementKind get() = KtRealSourceElementKind

    override fun realElement(): KtSourceElement {
        return this
    }

}

/**
 * Checking for [KtFakePsiSourceElement] only works for PSI sources.
 *
 * To check for a fake source regardless of source type, check if [KtSourceElement.kind] is a [KtFakeSourceElementKind].
 */
@RequiresOptIn
annotation class SuspiciousFakeSourceCheck

@SuspiciousFakeSourceCheck
open class KtFakePsiSourceElement(
    psi: PsiElement,
    override val kind: KtFakeSourceElementKind,
) : KtPsiSourceElement(psi) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as KtFakePsiSourceElement

        if (kind != other.kind) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + kind.hashCode()
        return result
    }
}

@SuspiciousFakeSourceCheck
class KtFakePsiSourceElementWithCustomOffsetStrategy(
    psi: PsiElement,
    kind: KtFakeSourceElementKind,
    val strategy: KtSourceElementOffsetStrategy.Custom,
) : KtFakePsiSourceElement(psi, kind) {
    override val startOffset: Int
        get() = strategy.startOffset

    override val endOffset: Int
        get() = strategy.endOffset

    override fun equals(other: Any?): Boolean = this === other ||
            other is KtFakePsiSourceElementWithCustomOffsetStrategy &&
            super.equals(other) &&
            strategy == other.strategy

    override fun hashCode(): Int = 31 * super.hashCode() + strategy.hashCode()
}

val AbstractKtSourceElement?.psi: PsiElement? get() = (this as? KtPsiSourceElement)?.psi

@Suppress("NOTHING_TO_INLINE")
inline fun PsiElement.toKtPsiSourceElement(kind: KtSourceElementKind = KtRealSourceElementKind): KtPsiSourceElement = when (kind) {
    is KtRealSourceElementKind -> KtRealPsiSourceElement(this)
    is KtFakeSourceElementKind -> KtFakePsiSourceElement(this, kind)
}

fun KtSourceElement?.hasUnwrappableAsExplicitReceiver(): Boolean {
    return when (this) {
        is KtLightSourceElement -> lighterASTNode.getExplicitReceiverOfDotQualified(treeStructure)?.tokenType in UNWRAPPABLE_TOKEN_TYPES
        is KtPsiSourceElement -> psi.getExplicitReceiverOfDotQualified()?.elementType in UNWRAPPABLE_TOKEN_TYPES
        else -> false
    }
}

/**
 * This function should only be called for a source element corresponding to
 * an assignment/assignment operator call/increment or a decrement operator.
 */
fun KtSourceElement?.hasUnwrappableAsAssignmentLhs(): Boolean {
    if (this == null) {
        return false
    }

    val node = psi?.getAssignmentLhsIfUnwrappable()
        ?: lighterASTNode.getAssignmentLhsIfUnwrappable(treeStructure)

    return node != null
}

