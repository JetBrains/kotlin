/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.optimization.common.FastMethodAnalyzer
import org.jetbrains.kotlin.codegen.optimization.common.InsnSequence
import org.jetbrains.kotlin.codegen.optimization.common.isMeaningful
import org.jetbrains.kotlin.codegen.optimization.nullCheck.isCheckParameterIsNotNull
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.*

fun MethodNode.remove(instructions: Sequence<AbstractInsnNode>) =
    instructions.forEach {
        this@remove.instructions.remove(it)
    }

fun MethodNode.remove(instructions: Collection<AbstractInsnNode>) {
    instructions.forEach {
        this@remove.instructions.remove(it)
    }
}

fun MethodNode.findCapturedFieldAssignmentInstructions(): Sequence<FieldInsnNode> {
    return InsnSequence(instructions).filterIsInstance<FieldInsnNode>().filter { fieldNode ->
        //filter captured field assignment
        //  aload 0
        //  aload x
        //  PUTFIELD $fieldName

        val prevPrev = fieldNode.previous?.previous as? VarInsnNode

        fieldNode.opcode == Opcodes.PUTFIELD &&
                isCapturedFieldName(fieldNode.name) &&
                fieldNode.previous is VarInsnNode && prevPrev != null && prevPrev.`var` == 0
    }
}

fun AbstractInsnNode.getNextMeaningful(): AbstractInsnNode? {
    var result: AbstractInsnNode? = next
    while (result != null && !result.isMeaningful) {
        result = result.next
    }
    return result
}

// Interpreter, that analyzes functional arguments only, to replace SourceInterpreter, since SourceInterpreter's merge has O(N²) complexity

internal class FunctionalArgumentValue(
    val functionalArgument: FunctionalArgument, basicValue: BasicValue?
) : BasicValue(basicValue?.type) {
    override fun toString(): String = "$functionalArgument"
}

val BasicValue?.functionalArgument
    get() = (this as? FunctionalArgumentValue)?.functionalArgument

internal class FunctionalArgumentInterpreter(private val inliner: MethodInliner) : BasicInterpreter(Opcodes.API_VERSION) {
    override fun unaryOperation(insn: AbstractInsnNode, value: BasicValue): BasicValue? =
        wrapArgumentInValueIfNeeded(insn, super.unaryOperation(insn, value))

    override fun copyOperation(insn: AbstractInsnNode, value: BasicValue?): BasicValue? =
        wrapArgumentInValueIfNeeded(insn, super.copyOperation(insn, value))

    override fun newOperation(insn: AbstractInsnNode): BasicValue? =
        wrapArgumentInValueIfNeeded(insn, super.newOperation(insn))

    private fun wrapArgumentInValueIfNeeded(
        insn: AbstractInsnNode,
        basicValue: BasicValue?
    ): BasicValue? =
        inliner.getFunctionalArgumentIfExists(insn)
            ?.let { FunctionalArgumentValue(it, basicValue) }
            ?: basicValue

    override fun merge(v: BasicValue?, w: BasicValue?): BasicValue? =
        if (v is FunctionalArgumentValue && w is FunctionalArgumentValue && v.functionalArgument == w.functionalArgument) v
        else super.merge(v, w)
}

internal fun AbstractInsnNode.isAloadBeforeCheckParameterIsNotNull(): Boolean =
    opcode == Opcodes.ALOAD && next?.opcode == Opcodes.LDC && next?.next?.isCheckParameterIsNotNull() == true

// Interpreter, that analyzes only ALOAD_0s, which are used as continuation arguments

internal class Aload0BasicValue private constructor(val indices: Set<Int>) : BasicValue(AsmTypes.OBJECT_TYPE) {
    constructor(i: Int) : this(setOf(i)) {}

    operator fun plus(other: Aload0BasicValue) = Aload0BasicValue(indices + other.indices)
}

internal class Aload0Interpreter(private val node: MethodNode) : BasicInterpreter(Opcodes.API_VERSION) {
    override fun copyOperation(insn: AbstractInsnNode, value: BasicValue?): BasicValue? =
        when {
            insn.isAload0() -> Aload0BasicValue(node.instructions.indexOf(insn))
            insn.opcode == Opcodes.ALOAD -> if (value == null) null else BasicValue(value.type)
            else -> super.copyOperation(insn, value)
        }

    override fun merge(v: BasicValue?, w: BasicValue?): BasicValue =
        if (v is Aload0BasicValue && w is Aload0BasicValue) v + w else super.merge(v, w)
}

internal fun AbstractInsnNode.isAload0() = opcode == Opcodes.ALOAD && (this as VarInsnNode).`var` == 0

internal fun analyzeMethodNodeWithInterpreter(node: MethodNode, interpreter: BasicInterpreter): Array<out Frame<BasicValue>?> {
    val analyzer = object : FastMethodAnalyzer<BasicValue>("fake", node, interpreter) {
        override fun newFrame(nLocals: Int, nStack: Int): Frame<BasicValue> {

            return object : Frame<BasicValue>(nLocals, nStack) {
                @Throws(AnalyzerException::class)
                override fun execute(insn: AbstractInsnNode, interpreter: Interpreter<BasicValue>) {
                    // This can be a void non-local return from a non-void method; Frame#execute would throw and do nothing else.
                    if (insn.opcode == Opcodes.RETURN) return
                    super.execute(insn, interpreter)
                }
            }
        }
    }

    try {
        return analyzer.analyze()
    } catch (e: AnalyzerException) {
        throw RuntimeException(e)
    }
}