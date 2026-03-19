/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.load.java.structure.JavaRecordComponent
import org.jetbrains.kotlin.load.java.structure.JavaType

class JavaRecordComponentOverAst(
    node: JavaSyntaxNode,
    containingClass: JavaClassOverAst
) : JavaMemberOverAst(node, containingClass), JavaRecordComponent {

    override val type: JavaType
        get() {
            val typeNode = node.findChildByType("TYPE")
                ?: throw IllegalStateException("Record component must have a type: ${node.text}")
            return createJavaType(typeNode, containingClass.memberResolutionContext)
        }

    override val isVararg: Boolean
        get() {
            if (node.findChildByType("ELLIPSIS") != null) return true
            val typeNode = node.findChildByType("TYPE")
            return typeNode?.findChildByType("ELLIPSIS") != null
        }

    override val isFromSource: Boolean get() = true
}
