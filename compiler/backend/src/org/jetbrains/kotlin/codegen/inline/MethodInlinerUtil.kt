/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.codegen.optimization.common.InsnSequence
import org.jetbrains.kotlin.codegen.optimization.common.isMeaningful
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.*

fun parameterOffsets(isStatic: Boolean, valueParameters: List<JvmMethodParameterSignature>): Array<Int> {
    var nextOffset = if (isStatic) 0 else 1
    return Array(valueParameters.size) { index ->
        nextOffset.also {
            nextOffset += valueParameters[index].asmType.size
        }
    }
}

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

// Interpreter, that analyzes only functional arguments, to replace SourceInterpreter, since SourceInterpreter' merge has O(N²) complexity

internal open class FunctionalArgumentValue(
    val functionalArgument: FunctionalArgument, basicValue: BasicValue?
) : BasicValue(basicValue?.type) {
    override fun toString(): String = "$functionalArgument"
}

val BasicValue?.functionalArgument
    get() = (this as? FunctionalArgumentValue)?.functionalArgument

private class FunctionalArgumentInterpreter(
    private val inliner: MethodInliner, private val toDelete: MutableSet<AbstractInsnNode>
) : BasicInterpreter(Opcodes.ASM5) {

    override fun unaryOperation(insn: AbstractInsnNode, value: BasicValue): BasicValue? =
        markInstructionIfNeeded(insn, super.unaryOperation(insn, value))

    override fun copyOperation(insn: AbstractInsnNode, value: BasicValue?): BasicValue? {
        val basicValue = super.copyOperation(insn, value)
        // Paramter checks are processed separately
        if (insn.next?.opcode == Opcodes.LDC && isParameterNullabilityCheck(insn.next?.next)) {
            return basicValue
        }
        if (value.functionalArgument is LambdaInfo) {
            // SWAP and ASTORE
            toDelete.add(insn)
            return value
        }
        return markInstructionIfNeeded(insn, basicValue)
    }

    private fun markInstructionIfNeeded(
        insn: AbstractInsnNode,
        basicValue: BasicValue?
    ): BasicValue? {
        val functionalArgument = inliner.getFunctionalArgumentIfExists(insn)
        return if (functionalArgument != null) {
            if (functionalArgument is LambdaInfo) {
                toDelete.add(insn)
            }
            FunctionalArgumentValue(functionalArgument, basicValue)
        } else basicValue
    }

    override fun newOperation(insn: AbstractInsnNode): BasicValue? =
        markInstructionIfNeeded(insn, super.newOperation(insn))

    override fun merge(v: BasicValue?, w: BasicValue?): BasicValue? =
        if (v is FunctionalArgumentValue && w is FunctionalArgumentValue && v.functionalArgument == w.functionalArgument) v
        else super.merge(v, w)
}

fun isParameterNullabilityCheck(methodInsnNode: AbstractInsnNode?): Boolean =
    methodInsnNode is MethodInsnNode && methodInsnNode.owner == IntrinsicMethods.INTRINSICS_CLASS_NAME &&
            (methodInsnNode.name == "checkParameterIsNotNull" || methodInsnNode.name == "checkNotNullParameter")

internal fun analyzeMethodWithFunctionalArgumentInterpreter(
    inliner: MethodInliner,
    node: MethodNode,
    toDelete: MutableSet<AbstractInsnNode>
): Array<out Frame<BasicValue>?> {
    val analyzer = object : Analyzer<BasicValue>(FunctionalArgumentInterpreter(inliner, toDelete)) {
        override fun newFrame(nLocals: Int, nStack: Int): Frame<BasicValue> {

            return object : Frame<BasicValue>(nLocals, nStack) {
                @Throws(AnalyzerException::class)
                override fun execute(insn: AbstractInsnNode, interpreter: Interpreter<BasicValue>) {
                    if (node.name == "waitForEx\$default" && node.instructions.indexOf(insn) == 18) {
                        {}()
                    }
                    // This can be a void non-local return from a non-void method; Frame#execute would throw and do nothing else.
                    if (insn.opcode == Opcodes.RETURN) return
                    super.execute(insn, interpreter)
                }
            }
        }
    }

    try {
        return analyzer.analyze("fake", node)
    } catch (e: AnalyzerException) {
        throw RuntimeException(e)
    }
}