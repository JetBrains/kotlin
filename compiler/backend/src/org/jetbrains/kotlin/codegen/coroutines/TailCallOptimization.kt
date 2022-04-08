/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.inline.isInlineMarker
import org.jetbrains.kotlin.codegen.optimization.boxing.isUnitInstance
import org.jetbrains.kotlin.codegen.optimization.common.ControlFlowGraph
import org.jetbrains.kotlin.codegen.optimization.common.isMeaningful
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicInterpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue

internal fun MethodNode.allSuspensionPointsAreTailCalls(suspensionPoints: List<SuspensionPoint>, optimizeReturnUnit: Boolean): Boolean {
    val frames = MethodTransformer.analyze("fake", this, TcoInterpreter(suspensionPoints))
    val controlFlowGraph = ControlFlowGraph.build(this)

    fun AbstractInsnNode.isSafe(): Boolean =
        !isMeaningful || opcode in SAFE_OPCODES || isInvisibleInDebugVarInsn(this@allSuspensionPointsAreTailCalls) || isInlineMarker(this)

    fun AbstractInsnNode.transitiveSuccessorsAreSafeOrReturns(): Boolean {
        val visited = mutableSetOf(this)
        val stack = mutableListOf(this)
        while (stack.isNotEmpty()) {
            val insn = stack.popLast()
            // In Unit-returning functions, the last statement is followed by POP + GETSTATIC Unit.INSTANCE
            // if it is itself not Unit-returning.
            if (insn.opcode == Opcodes.ARETURN || (optimizeReturnUnit && insn.isPopBeforeReturnUnit)) {
                if (frames[instructions.indexOf(insn)]?.top() !is FromSuspensionPointValue?) {
                    return false
                }
            } else if (insn !== this && !insn.isSafe()) {
                return false
            } else {
                for (nextIndex in controlFlowGraph.getSuccessorsIndices(insn)) {
                    val nextInsn = instructions.get(nextIndex)
                    if (visited.add(nextInsn)) {
                        stack.add(nextInsn)
                    }
                }
            }
        }
        return true
    }

    return suspensionPoints.all { suspensionPoint ->
        val index = instructions.indexOf(suspensionPoint.suspensionCallBegin)
        tryCatchBlocks.all { index < instructions.indexOf(it.start) || instructions.indexOf(it.end) <= index } &&
                suspensionPoint.suspensionCallEnd.transitiveSuccessorsAreSafeOrReturns()
    }
}

internal fun MethodNode.addCoroutineSuspendedChecks(suspensionPoints: List<SuspensionPoint>) {
    for (suspensionPoint in suspensionPoints) {
        if (suspensionPoint.suspensionCallEnd.nextMeaningful?.opcode == Opcodes.ARETURN) {
            // `if (x == COROUTINE_SUSPENDED) return x else return x` == `return x`
            continue
        }
        instructions.insert(suspensionPoint.suspensionCallEnd, withInstructionAdapter {
            val label = Label()
            dup()
            loadCoroutineSuspendedMarker()
            ifacmpne(label)
            areturn(AsmTypes.OBJECT_TYPE)
            mark(label)
        })
    }
}

private tailrec fun AbstractInsnNode?.skipUntilMeaningful(): AbstractInsnNode? = when {
    this == null -> null
    opcode == Opcodes.NOP || !isMeaningful -> next.skipUntilMeaningful()
    opcode == Opcodes.GOTO -> (this as JumpInsnNode).label.skipUntilMeaningful()
    else -> this
}

private val AbstractInsnNode.nextMeaningful: AbstractInsnNode?
    get() = next.skipUntilMeaningful()

private val AbstractInsnNode.isReturnUnit: Boolean
    get() = isUnitInstance() && nextMeaningful?.opcode == Opcodes.ARETURN

private val AbstractInsnNode.isPopBeforeReturnUnit: Boolean
    get() = opcode == Opcodes.POP && nextMeaningful?.isReturnUnit == true

private fun AbstractInsnNode?.isInvisibleInDebugVarInsn(methodNode: MethodNode): Boolean {
    val insns = methodNode.instructions
    val index = insns.indexOf(this)
    return (this is VarInsnNode && methodNode.localVariables.none {
        it.index == `var` && index in it.start.let(insns::indexOf)..it.end.let(insns::indexOf)
    })
}

private val SAFE_OPCODES = buildSet {
    add(Opcodes.NOP)
    addAll(Opcodes.POP..Opcodes.SWAP) // POP*, DUP*, SWAP
    addAll(Opcodes.IFEQ..Opcodes.GOTO) // IF*, GOTO
    // CHECKCAST is technically not safe (can throw), but should be unless the type system is holey.
    // Treating it as safe permits optimizing functions where a non-Any returning suspend function
    // call is in a tail position (in bytecode they all return Object, so a cast is sometimes inserted).
    add(Opcodes.CHECKCAST)
}

private object FromSuspensionPointValue : BasicValue(AsmTypes.OBJECT_TYPE) {
    override fun equals(other: Any?): Boolean = other is FromSuspensionPointValue
}

private fun BasicValue?.toFromSuspensionPoint(): BasicValue? = if (this?.type?.sort == Type.OBJECT) FromSuspensionPointValue else this

private class TcoInterpreter(private val suspensionPoints: List<SuspensionPoint>) : BasicInterpreter(Opcodes.API_VERSION) {
    override fun copyOperation(insn: AbstractInsnNode, value: BasicValue?): BasicValue? {
        return super.copyOperation(insn, value).convert(insn)
    }

    private fun BasicValue?.convert(insn: AbstractInsnNode): BasicValue? = if (insn in suspensionPoints) toFromSuspensionPoint() else this

    override fun naryOperation(insn: AbstractInsnNode, values: MutableList<out BasicValue?>?): BasicValue? {
        return super.naryOperation(insn, values).convert(insn)
    }

    override fun ternaryOperation(insn: AbstractInsnNode, value1: BasicValue?, value2: BasicValue?, value3: BasicValue?): BasicValue? {
        return super.ternaryOperation(insn, value1, value2, value3).convert(insn)
    }

    override fun merge(value1: BasicValue?, value2: BasicValue?): BasicValue {
        return if (value1 is FromSuspensionPointValue || value2 is FromSuspensionPointValue) FromSuspensionPointValue
        else super.merge(value1, value2)
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: BasicValue?): BasicValue? {
        // Assume, that CHECKCAST Object does not break tail-call optimization
        if (value is FromSuspensionPointValue && insn.opcode == Opcodes.CHECKCAST) {
            return value
        }
        return super.unaryOperation(insn, value).convert(insn)
    }

    override fun binaryOperation(insn: AbstractInsnNode, value1: BasicValue?, value2: BasicValue?): BasicValue? {
        return super.binaryOperation(insn, value1, value2).convert(insn)
    }

    override fun newOperation(insn: AbstractInsnNode): BasicValue? {
        return super.newOperation(insn).convert(insn)
    }
}
