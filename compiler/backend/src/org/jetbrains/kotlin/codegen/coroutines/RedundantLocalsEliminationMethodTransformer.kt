/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.optimization.boxing.isUnitInstance
import org.jetbrains.kotlin.codegen.optimization.common.MethodAnalyzer
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.common.removeAll
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.LabelNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceInterpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceValue

// A [SourceInterpreter] which keeps track of all use sites for "GETSTATIC Unit" instructions.
private class SourceInterpreterWithUnitUsage : SourceInterpreter(Opcodes.API_VERSION) {
    val unitUsageInformation = mutableMapOf<AbstractInsnNode, MutableSet<AbstractInsnNode>>()

    private fun markUnitUsage(use: AbstractInsnNode, defs: SourceValue?) {
        defs?.insns?.forEach { def ->
            if (def.isUnitInstance()) {
                unitUsageInformation.getOrPut(def) { mutableSetOf() } += use
            }
        }
    }

    fun run(internalClassName: String, methodNode: MethodNode): SourceFrames {
        val sourceFrames = MethodAnalyzer<SourceValue>(internalClassName, methodNode, this).analyze()
        // The ASM analyzer does not visit POP instructions, so we do so here.
        for ((insn, frame) in methodNode.instructions.asSequence().zip(sourceFrames.asSequence())) {
            if (frame != null && insn.opcode == Opcodes.POP) {
                markUnitUsage(insn, frame.top())
            }
        }
        return sourceFrames
    }

    override fun copyOperation(insn: AbstractInsnNode, value: SourceValue?): SourceValue? {
        markUnitUsage(insn, value)
        return super.copyOperation(insn, value)
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: SourceValue?): SourceValue {
        markUnitUsage(insn, value)
        return super.unaryOperation(insn, value)
    }

    override fun binaryOperation(insn: AbstractInsnNode, value1: SourceValue?, value2: SourceValue?): SourceValue {
        markUnitUsage(insn, value1)
        markUnitUsage(insn, value2)
        return super.binaryOperation(insn, value1, value2)
    }

    override fun ternaryOperation(insn: AbstractInsnNode, value1: SourceValue?, value2: SourceValue?, value3: SourceValue?): SourceValue {
        markUnitUsage(insn, value1)
        markUnitUsage(insn, value2)
        markUnitUsage(insn, value3)
        return super.ternaryOperation(insn, value1, value2, value3)
    }

    override fun naryOperation(insn: AbstractInsnNode, values: List<SourceValue>?): SourceValue {
        values?.forEach { markUnitUsage(insn, it) }
        return super.naryOperation(insn, values)
    }
}

/**
 * This pass removes unused Unit values and locals. These typically occur as a result of inlining and could
 * end up spilling into the continuation object or breaking tail-call elimination.
 *
 * Concretely, we remove "GETSTATIC kotlin/Unit.INSTANCE" instructions if they are unused, or all uses are either
 * POP instructions, or ASTORE instructions to locals which are never read and are not named local variables.
 *
 * This pass does not touch [suspensionPoints], as later passes rely on the bytecode patterns around suspension points.
 */
internal class RedundantLocalsEliminationMethodTransformer(private val suspensionPoints: List<SuspensionPoint>) : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val interpreter = SourceInterpreterWithUnitUsage()
        val sourceFrames = interpreter.run(internalClassName, methodNode)

        // Mark all ASTORE instructions which are read later or are visible in the debugger.
        val liveStores = mutableSetOf<AbstractInsnNode>()
        val toDelete = mutableSetOf<AbstractInsnNode>()
        for ((insn, frame) in methodNode.instructions.asSequence().zip(sourceFrames.asSequence())) {
            if (frame == null) {
                // Mark all unreachable instructions for deletion. That is except for labels, which may be attached to a local variable.
                if (insn !is LabelNode) {
                    toDelete += insn
                }
            } else if (insn is VarInsnNode) {
                when (insn.opcode) {
                    Opcodes.ALOAD ->
                        liveStores += frame.getLocal(insn.`var`)?.insns ?: continue
                    Opcodes.ASTORE ->
                        // Stores to local variables need to be preserved, since they are visible in the debugger.
                        if (methodNode.localVariables?.any { it.index == insn.`var` } == true)
                            liveStores += insn
                }
            }
        }

        // Mark all "GETSTATIC kotlin/Unit.INSTANCE" instructions which are only used as the
        // single argument to dead stores for deletion.
        outer@ for ((unit, uses) in interpreter.unitUsageInformation) {
            if (unit in suspensionPoints)
                continue

            for (use in uses) {
                if ((use.opcode != Opcodes.ASTORE || use in liveStores) && use.opcode != Opcodes.POP) {
                    continue@outer
                }

                val index = methodNode.instructions.indexOf(use)
                if (sourceFrames[index]?.top()?.insns?.size != 1) {
                    continue@outer
                }
            }
            toDelete += unit
            toDelete += uses
        }

        methodNode.instructions.removeAll(toDelete)
    }
}
