/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.model

import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.load.java.structure.JavaElement

abstract class JavaElementOverAst(
    val node: JavaLightNode,
    val tree: JavaLightTree,
) : JavaElement {
    override fun equals(other: Any?): Boolean =
        other is JavaElementOverAst && node == other.node && tree === other.tree

    override fun hashCode(): Int = node.hashCode()

    override fun toString(): String = tree.getType(node).toString()
}
