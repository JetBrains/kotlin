/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList

class InsnSequence(val from: AbstractInsnNode, val to: AbstractInsnNode?) : Sequence<AbstractInsnNode> {
    constructor(insnList: InsnList) : this(insnList.first, null)

    override fun iterator(): Iterator<AbstractInsnNode> {
        return object : Iterator<AbstractInsnNode> {
            var current: AbstractInsnNode? = from
            override fun next(): AbstractInsnNode {
                val result = current
                current = current!!.next
                return result!!
            }

            override fun hasNext() = current != to
        }
    }
}

fun InsnList.asSequence(): Sequence<AbstractInsnNode> =
    if (size() == 0) emptySequence() else InsnSequence(this)
