/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.InsnSequence
import org.jetbrains.kotlin.codegen.optimization.common.removeAll
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.LabelNode
import org.jetbrains.org.objectweb.asm.tree.LineNumberNode
import org.jetbrains.org.objectweb.asm.tree.LocalVariableNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class GeneratedCodeMarkers(
    val checkContinuation: Int,
    val lambdaArgumentsUnspilling: Int,
    val tableswitch: Int,
    val checkResult: Int,
    val checkCOROUTINE_SUSPENDED: Int,
    val unreachable: Int,
) {
    // Add records to LVT with the following pattern
    //  $ecd$<generatedCodeMarker>$<lineNumber> I
    // which span the whole function and initialized with zero at the beginning of the function.
    fun addFakeVariablesToLVTAndInitializeThem(methodNode: MethodNode, isForNamedFunction: Boolean) {
        val start = methodNode.getOrCreateStartingLabel()
        val end = methodNode.getOrCreateEndingLabel()
        if (isForNamedFunction) {
            assert(checkContinuation != -1) { "checkContinuation marker should be defined" }
            addLocalVariableAndInitializeIt(
                methodNode, start, end, "checkContinuation\$$checkContinuation", methodNode.maxLocals++
            )
        } else {
            assert(lambdaArgumentsUnspilling != -1) {
                "lambdaArgumentsUnspilling marker should be defined"
            }
            addLocalVariableAndInitializeIt(
                methodNode, start, end, "lambdaArgumentsUnspilling\$$lambdaArgumentsUnspilling", methodNode.maxLocals++
            )
        }
        addLocalVariableAndInitializeIt(methodNode, start, end, "tableswitch\$$tableswitch", methodNode.maxLocals++)
        addLocalVariableAndInitializeIt(methodNode, start, end, "checkResult\$$checkResult", methodNode.maxLocals++)
        addLocalVariableAndInitializeIt(methodNode, start, end, "checkCOROUTINE_SUSPENDED\$$checkCOROUTINE_SUSPENDED", methodNode.maxLocals++)
        addLocalVariableAndInitializeIt(methodNode, start, end, "unreachable\$$unreachable", methodNode.maxLocals++)
    }

    private fun addLocalVariableAndInitializeIt(
        methodNode: MethodNode, start: LabelNode, end: LabelNode, suffix: String, index: Int
    ) {
        methodNode.instructions.insertBefore(start, withInstructionAdapter {
            iconst(0)
            store(index, Type.INT_TYPE)
        })

        methodNode.localVariables.add(LocalVariableNode("\$ecd\$$suffix", "I", null, start, end, index))
    }

    companion object {
        fun fillOutMarkersAndCleanUpMethodNode(methodNode: MethodNode): GeneratedCodeMarkers = GeneratedCodeMarkers(
            findLinenumberAndRemoveCodeAddedByInliner(methodNode, "checkContinuation"),
            findLinenumberAndRemoveCodeAddedByInliner(methodNode, "lambdaArgumentsUnspilling"),
            findLinenumberAndRemoveCodeAddedByInliner(methodNode, "tableswitch"),
            findLinenumberAndRemoveCodeAddedByInliner(methodNode, "checkResult"),
            findLinenumberAndRemoveCodeAddedByInliner(methodNode, "checkCOROUTINE_SUSPENDED"),
            findLinenumberAndRemoveCodeAddedByInliner(methodNode, "unreachable"),
        )

        private fun findLinenumberAndRemoveCodeAddedByInliner(methodNode: MethodNode, suffix: String): Int {
            var localVar = methodNode.localVariables.find { it.name.startsWith("\$i\$f\$$suffix") } // Ignore scope numbers
            var result = -1
            if (localVar != null) {
                val label = localVar.start
                val linenumber = label.next as? LineNumberNode
                result = linenumber?.line ?: -1

                /* Inlined bytecode looks like
                 * LN:
                 *   NOP
                 *   ICONST_0
                 *   ISTORE
                 * L<start>:
                 *   LINENUMBER
                 *   NOP
                 *   GOTO
                 * L<end>:
                 *
                 * Because before L<start> label we initialize fake inliner variable $i$f.
                 * Remove this initialization as well
                 */
                var start = label as AbstractInsnNode
                if (start.previous?.opcode == Opcodes.ISTORE && start.previous.previous?.opcode == Opcodes.ICONST_0) {
                    start = start.previous.previous
                }
                if (start.previous?.opcode == Opcodes.NOP) {
                    start = start.previous
                }

                methodNode.instructions.removeAll(InsnSequence(start, localVar.end).toList())
                methodNode.localVariables.remove(localVar)
            }
            return result
        }

        fun markFakeLineNumber(mv: InstructionAdapter, line: Int?) {
            if (line == null) return
            val label = Label()
            mv.mark(label)
            mv.visitLineNumber(line, label)
        }
    }
}