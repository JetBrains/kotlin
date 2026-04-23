/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.model

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.load.java.structure.JavaRecordComponent
import org.jetbrains.kotlin.load.java.structure.JavaType

class JavaRecordComponentOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    containingClass: JavaClassOverAst,
) : JavaMemberOverAst(node, tree, containingClass), JavaRecordComponent {

    override val type: JavaType
        get() {
            val typeNode = tree.findChildByType(node, JavaSyntaxElementType.TYPE)
                ?: throw IllegalStateException("Record component must have a type: ${tree.getText(node)}")
            return createJavaType(typeNode, tree, containingClass.memberResolutionContext)
        }

    override val isVararg: Boolean
        get() {
            if (tree.findChildByType(node, JavaSyntaxTokenType.ELLIPSIS) != null) return true
            val typeNode = tree.findChildByType(node, JavaSyntaxElementType.TYPE)
            return typeNode?.let { tree.findChildByType(it, JavaSyntaxTokenType.ELLIPSIS) } != null
        }

    override val isFromSource: Boolean get() = true
}
