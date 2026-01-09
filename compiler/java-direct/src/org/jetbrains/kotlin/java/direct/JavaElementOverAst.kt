/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.load.java.structure.JavaElement

abstract class JavaElementOverAst(
    val node: JavaSyntaxNode,
    val source: CharSequence
) : JavaElement {
    override fun equals(other: Any?): Boolean =
        other is JavaElementOverAst && node == other.node

    override fun hashCode(): Int = node.hashCode()

    override fun toString(): String = node.type.toString()
}
