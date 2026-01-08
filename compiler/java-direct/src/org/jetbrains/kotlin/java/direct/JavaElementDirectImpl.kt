/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.load.java.structure.JavaElement

abstract class JavaElementDirectImpl(
    val node: DirectSyntaxNode,
    val source: CharSequence
) : JavaElement {
    override fun equals(other: Any?): Boolean =
        other is JavaElementDirectImpl && node == other.node

    override fun hashCode(): Int = node.hashCode()

    override fun toString(): String = node.type.toString()
}
