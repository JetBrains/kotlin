/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.model

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceElementKind
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.fir.java.JavaDirectSourceElementOwner
import org.jetbrains.kotlin.java.direct.parse.JavaLightAstNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.load.java.structure.JavaElement

abstract class JavaElementOverAst(
    val node: JavaLightNode,
    val tree: JavaLightTree,
) : JavaElement, JavaDirectSourceElementOwner {

    open val isFromSource: Boolean get() = true

    /**
     * Text of this element's own IDENTIFIER child node, or `null` if it has none.
     */
    protected fun identifierText(): String? =
        tree.findChildByType(node, JavaSyntaxTokenType.IDENTIFIER)?.let { tree.getText(it).toString() }

    /**
     * This element's own MODIFIER_LIST child, if present. `open` so [JavaClassOverAst] can cache it
     * lazily; the default plain getter is reused by members and value parameters.
     */
    protected open val modifierList: JavaLightNode?
        get() = tree.findChildByType(node, JavaSyntaxElementType.MODIFIER_LIST)

    override fun toKtSourceElement(kind: KtSourceElementKind): KtSourceElement =
        KtLightSourceElement(
            JavaLightAstNode(tree, node),
            tree.getStartOffset(node),
            tree.getEndOffset(node),
            tree.lightSourceTreeStructure,
            kind,
        )

    override fun equals(other: Any?): Boolean =
        other is JavaElementOverAst && node == other.node && tree === other.tree

    override fun hashCode(): Int = node.hashCode()

    override fun toString(): String =
        tree.getType(node).toString()
}
