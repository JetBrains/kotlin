/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.optimization.boxing.isUnitInstance
import org.jetbrains.kotlin.codegen.optimization.common.MethodAnalyzer
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.common.removeAll
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.LabelNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicInterpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import java.util.*

/**
 * This pass removes unused Unit values. These typically occur as a result of inlining and could end up spilling
 * into the continuation object or break tail-call elimination.
 *
 * Concretely, we remove "GETSTATIC kotlin/Unit.INSTANCE" instructions if they are unused, or all uses are either
 * POP instructions, or ASTORE instructions to locals which are never read and are not named local variables.
 *
 * This pass does not touch [suspensionPoints], as later passes rely on the bytecode patterns around suspension points.
 */
internal class RedundantLocalsEliminationMethodTransformer(private val suspensionPoints: List<SuspensionPoint>) : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val interpreter = UnitSourceInterpreter(methodNode.localVariables?.mapTo(mutableSetOf()) { it.index } ?: setOf())
        val frames = interpreter.run(internalClassName, methodNode)

        // Mark all unused instructions for deletion (except for labels which may be used in debug information)
        val toDelete = mutableSetOf<AbstractInsnNode>()
        methodNode.instructions.asSequence().zip(frames.asSequence()).mapNotNullTo(toDelete) { (insn, frame) ->
            insn.takeIf { frame == null && insn !is LabelNode }
        }

        // Mark all spillable "GETSTATIC kotlin/Unit.INSTANCE" instructions for deletion
        for ((unit, uses) in interpreter.unitUsageInformation) {
            if (unit !in interpreter.unspillableUnitValues && unit !in suspensionPoints) {
                toDelete += unit
                toDelete += uses
            }
        }

        methodNode.instructions.removeAll(toDelete)
    }
}

// A version of SourceValue which inherits from BasicValue and is only used for Unit values.
private class UnitValue(val insns: Set<AbstractInsnNode>) : BasicValue(AsmTypes.OBJECT_TYPE) {
    constructor(insn: AbstractInsnNode) : this(Collections.singleton(insn))

    override fun equals(other: Any?): Boolean = other is UnitValue && insns == other.insns
    override fun hashCode() = Objects.hash(insns)
    override fun toString() = "U"
}

// A specialized SourceInterpreter which only keeps track of the use sites for Unit values which are exclusively used as
// arguments to POP and unused ASTORE instructions.
private class UnitSourceInterpreter(private val localVariables: Set<Int>) : BasicInterpreter(Opcodes.API_VERSION) {
    // All unit values with visible use-sites.
    val unspillableUnitValues = mutableSetOf<AbstractInsnNode>()

    // Map from unit values to ASTORE/POP use-sites.
    val unitUsageInformation = mutableMapOf<AbstractInsnNode, MutableSet<AbstractInsnNode>>()

    private fun markUnspillable(value: BasicValue?) =
        value?.safeAs<UnitValue>()?.let { unspillableUnitValues += it.insns }

    private fun collectUnitUsage(use: AbstractInsnNode, value: UnitValue) {
        for (def in value.insns) {
            if (def !in unspillableUnitValues) {
                unitUsageInformation.getOrPut(def) { mutableSetOf() } += use
            }
        }
    }

    fun run(internalClassName: String, methodNode: MethodNode): Array<Frame<BasicValue>?> {
        val frames = MethodAnalyzer<BasicValue>(internalClassName, methodNode, this).analyze()
        // The ASM analyzer does not visit POP instructions, so we do so here.
        for ((insn, frame) in methodNode.instructions.asSequence().zip(frames.asSequence())) {
            if (frame != null && insn.opcode == Opcodes.POP) {
                val value = frame.top()
                value.safeAs<UnitValue>()?.let { collectUnitUsage(insn, it) }
            }
        }
        return frames
    }

    override fun newOperation(insn: AbstractInsnNode?): BasicValue =
        if (insn?.isUnitInstance() == true) UnitValue(insn) else super.newOperation(insn)

    override fun copyOperation(insn: AbstractInsnNode, value: BasicValue?): BasicValue? {
        if (value is UnitValue) {
            if (insn is VarInsnNode && insn.opcode == Opcodes.ASTORE && insn.`var` !in localVariables) {
                collectUnitUsage(insn, value)
                // We track the stored value in case it is subsequently read.
                return value
            }
            unspillableUnitValues += value.insns
        }
        return super.copyOperation(insn, value)
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: BasicValue?): BasicValue? {
        markUnspillable(value)
        return super.unaryOperation(insn, value)
    }

    override fun binaryOperation(insn: AbstractInsnNode, value1: BasicValue?, value2: BasicValue?): BasicValue? {
        markUnspillable(value1)
        markUnspillable(value2)
        return super.binaryOperation(insn, value1, value2)
    }

    override fun ternaryOperation(insn: AbstractInsnNode, value1: BasicValue?, value2: BasicValue?, value3: BasicValue?): BasicValue? {
        markUnspillable(value1)
        markUnspillable(value2)
        markUnspillable(value3)
        return super.ternaryOperation(insn, value1, value2, value3)
    }

    override fun naryOperation(insn: AbstractInsnNode, values: List<BasicValue>?): BasicValue? {
        values?.forEach(this::markUnspillable)
        return super.naryOperation(insn, values)
    }

    override fun merge(value1: BasicValue?, value2: BasicValue?): BasicValue? =
        if (value1 is UnitValue && value2 is UnitValue) {
            val newValue = UnitValue(value1.insns.union(value2.insns))
            if (newValue.insns.any { it in unspillableUnitValues }) {
                markUnspillable(newValue)
            }
            newValue
        } else {
            // Mark unit values as unspillable if we merge them with non-unit values here.
            // This is conservative since the value could turn out to be unused.
            markUnspillable(value1)
            markUnspillable(value2)
            super.merge(value1, value2)
        }
}
