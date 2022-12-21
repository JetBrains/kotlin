/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization

import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.Companion.isOperationReifiedMarker
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.MethodNode

object RedundantCheckcastsBeforeAastoreMethodTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val iter = methodNode.instructions.iterator()
        while (iter.hasNext()) {
            val insn = iter.next()
            if (insn.opcode == Opcodes.CHECKCAST && insn.next?.opcode == Opcodes.AASTORE) {
                val isReified = isOperationReifiedMarker(insn.previous)
                iter.remove()
                if (isReified) {
                    for (i in 1..3) {
                        iter.previous()
                        iter.remove()
                    }
                }
            }
        }
    }
}