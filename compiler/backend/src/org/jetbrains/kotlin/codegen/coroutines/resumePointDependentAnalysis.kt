/*
 * Copyright 2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.inline.isAfterSuspendMarker
import org.jetbrains.kotlin.codegen.optimization.common.FastMethodAnalyzer
import org.jetbrains.kotlin.utils.forEachBit
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.Value
import java.util.*

// Predicts state of locals slots after resuming on `i-th` suspension point in case if no spills/restores
// were added. Used to find out what slots would require extra care (like default-initialization on restore point)
// if the following conditions are met:
// - the slot is "dead" on SP_i and is not selected for spill
// - the slot is "dead" on SP_j BUT is selected for spill there
// - there is a path SP_i -> SP_j with no STORE to that slot
internal fun performUninitializedAfterResumeVariablesAnalysis(
    suspensionPoints: List<SuspensionPoint>,
    methodNode: MethodNode,
    thisName: String,
): Array<out Frame<ResumeDependentValue>?> {
    val context = SuspensionPointsContext(suspensionPoints)
    // NOTE: exception edges shall not be "pruned" as path from SP to exception handlers are important for this analysis
    val analyzer = FastMethodAnalyzer(
        thisName, methodNode, ResumeDependentInterpeter(context), false
    ) { nLocals, nStack -> ResumeDependentFrame(context, nLocals) }
    return analyzer.analyze()
}

// States of a single variable on some instruction for each of "resumed on SP_i" cases
internal class ResumeDependentValue(val states: Array<VariableState>) : Value {

    constructor(suspensionPointsCount: Int) : this(Array(suspensionPointsCount) { VariableState.UNKNOWN })

    fun withState(spIndex: Int, state: VariableState) : ResumeDependentValue =
        if (states[spIndex] == state) this else ResumeDependentValue(states.copyOf().apply { set(spIndex, state) })

    // size is ignored for such special Value
    override fun getSize(): Int = 1

    override fun toString(): String = states.joinToString(separator = "", prefix="[", postfix = "]") { it.toString() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResumeDependentValue) return false

        return states.contentEquals(other.states)
    }

    override fun hashCode(): Int {
        return states.contentHashCode()
    }

    // Variable state on some instruction in case of resume on a given suspension point
    enum class VariableState {
        // not analyzed yet, or no path from the suspension point
        UNKNOWN,
        // initialized by some STORE on each path from the suspension point to the instruction
        INITIALIZED,
        // not initialized on at least one path from the suspension point to the instruction
        UNINITIALIZED;

        fun isUnitialized() = this == UNINITIALIZED

        fun merge(other: VariableState): VariableState =
            when {
                this == UNKNOWN -> other
                this == UNINITIALIZED || other == UNINITIALIZED -> UNINITIALIZED
                else -> this
            }

        override fun toString(): String =
            when (this) {
                UNKNOWN -> "?"
                UNINITIALIZED -> "!"
                INITIALIZED -> "+"
            }
    }
}

// immmutable shared part of the frames used during this analysis
private class SuspensionPointsContext(suspensionPoints: List<SuspensionPoint>) {
    init {
        check(suspensionPoints.isNotEmpty())
        check(suspensionPoints.all { isAfterSuspendMarker(it.suspensionCallEnd) })
    }

    val suspensionPointIndexByEnd: Map<AbstractInsnNode, Int> =
        suspensionPoints.withIndex().associate { (i, sp) -> sp.suspensionCallEnd to i }

    val suspensionPointsCount = suspensionPoints.size
}

private class ResumeDependentFrame(val context: SuspensionPointsContext, val maxLocals: Int): Frame<ResumeDependentValue>(maxLocals, 0) {
    val isAfterSuspensionPoint = BitSet(context.suspensionPointsCount)

    init {
        for (varIndex in 0 until maxLocals) {
            setLocal(varIndex, ResumeDependentValue(context.suspensionPointsCount))
        }
    }

    override fun toString(): String {
        return "After ${isAfterSuspensionPoint}, " + super.toString()
    }

    override fun init(frame: Frame<out ResumeDependentValue?>): Frame<ResumeDependentValue?>? {
        val other = frame as? ResumeDependentFrame ?: return super.init(frame)
        isAfterSuspensionPoint.clear()
        isAfterSuspensionPoint.or(other.isAfterSuspensionPoint)
        return super.init(frame)
    }

    override fun merge(
        frame: Frame<out ResumeDependentValue?>,
        interpreter: Interpreter<ResumeDependentValue?>?,
    ): Boolean {
        val other = frame as? ResumeDependentFrame ?: return super.merge(frame, interpreter)
        check(maxLocals == other.maxLocals)

        val changed = isAfterSuspensionPoint != other.isAfterSuspensionPoint
        isAfterSuspensionPoint.or(other.isAfterSuspensionPoint)

        return super.merge(frame, interpreter) || changed
    }

    override fun execute(insn: AbstractInsnNode, unused: Interpreter<ResumeDependentValue>?) {
        if (isAfterSuspendMarker(insn)) {
            context.suspensionPointIndexByEnd[insn]?.let {
                isAfterSuspensionPoint.set(it)
                setAllLocalsState(it, ResumeDependentValue.VariableState.UNINITIALIZED)
            }
        }
        when (insn.opcode) {
            in Opcodes.ISTORE..Opcodes.ASTORE -> {
                val varInsn = insn as VarInsnNode
                isAfterSuspensionPoint.forEachBit { setLocalState(varInsn.`var`, it, ResumeDependentValue.VariableState.INITIALIZED) }
            }
            Opcodes.IINC -> {
                val iincInsn = insn as IincInsnNode
                isAfterSuspensionPoint.forEachBit { setLocalState(iincInsn.`var`, it, ResumeDependentValue.VariableState.INITIALIZED) }
            }
        }
    }

    private fun setAllLocalsState(spIndex: Int, state: ResumeDependentValue.VariableState) {
        (0 until maxLocals).forEach { setLocalState(it, spIndex, state) }
    }

    private fun setLocalState(varIndex: Int, spIndex: Int, state: ResumeDependentValue.VariableState) {
        setLocal(varIndex, getLocal(varIndex).withState(spIndex, state))
    }
}

private class ResumeDependentInterpeter(val context: SuspensionPointsContext) : Interpreter<ResumeDependentValue>(Opcodes.API_VERSION) {
    // The only used methods in this interpreter are `newValue()` called during the parameters initialization, and `merge()`
    // No actual instructions interpretation is implemented or used, as it is frame-dependent, so is performed
    // solely in ResumeDependentFrame.execute() method
    override fun newValue(type: Type?): ResumeDependentValue = ResumeDependentValue(context.suspensionPointsCount)

    override fun merge(value1: ResumeDependentValue, value2: ResumeDependentValue): ResumeDependentValue {
        if (value1 == value2) return value1
        val mergedStates = (0 until context.suspensionPointsCount)
            .map { spIndex -> value1.states[spIndex].merge(value2.states[spIndex]) }
            .toTypedArray()
        return ResumeDependentValue(mergedStates)
    }

    override fun newOperation(insn: AbstractInsnNode?): ResumeDependentValue? = null
    override fun copyOperation(insn: AbstractInsnNode?, value: ResumeDependentValue?): ResumeDependentValue? = null
    override fun unaryOperation(insn: AbstractInsnNode?, value: ResumeDependentValue?): ResumeDependentValue? = null

    override fun binaryOperation(insn: AbstractInsnNode?,
                                 value1: ResumeDependentValue?,
                                 value2: ResumeDependentValue?,
    ): ResumeDependentValue? = null

    override fun ternaryOperation(
        insn: AbstractInsnNode?,
        value1: ResumeDependentValue?,
        value2: ResumeDependentValue?,
        value3: ResumeDependentValue?,
    ): ResumeDependentValue? = null

    override fun naryOperation(
        insn: AbstractInsnNode?,
        values: List<ResumeDependentValue?>?,
    ): ResumeDependentValue? = null

    override fun returnOperation(
        insn: AbstractInsnNode?,
        value: ResumeDependentValue?,
        expected: ResumeDependentValue?,
    ) {
        // No implementation needed
    }
}
