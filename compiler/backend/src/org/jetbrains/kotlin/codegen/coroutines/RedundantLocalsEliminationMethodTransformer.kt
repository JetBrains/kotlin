/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.inline.nodeText
import org.jetbrains.kotlin.codegen.optimization.boxing.isUnitInstance
import org.jetbrains.kotlin.codegen.optimization.common.MethodAnalyzer
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.common.removeAll
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicInterpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame

private class PossibleSpilledValue(val source: AbstractInsnNode, type: Type?) : BasicValue(type) {
    val usages = mutableSetOf<AbstractInsnNode>()

    override fun toString(): String = when {
        source.opcode == Opcodes.ALOAD -> "" + (source as VarInsnNode).`var`
        source.isUnitInstance() -> "U"
        else -> error("unreachable")
    }

    override fun equals(other: Any?): Boolean =
        other is PossibleSpilledValue && source == other.source

    override fun hashCode(): Int = super.hashCode() xor source.hashCode()
}

private object NonSpillableValue : BasicValue(AsmTypes.OBJECT_TYPE) {
    override fun equals(other: Any?): Boolean = other is NonSpillableValue

    override fun toString(): String = "N"
}

private object ConstructedValue : BasicValue(AsmTypes.OBJECT_TYPE) {
    override fun equals(other: Any?): Boolean = other is ConstructedValue

    override fun toString(): String = "C"
}

fun BasicValue?.nonspillable(): BasicValue? = if (this?.type?.sort == Type.OBJECT) NonSpillableValue else this

private class RedundantSpillingInterpreter : BasicInterpreter(Opcodes.API_VERSION) {
    val possibleSpilledValues = mutableSetOf<PossibleSpilledValue>()

    override fun newOperation(insn: AbstractInsnNode): BasicValue? {
        if (insn.opcode == Opcodes.NEW) return ConstructedValue
        val basicValue = super.newOperation(insn)
        return if (insn.isUnitInstance())
            // Unit instances come from inlining suspend functions returning Unit.
            // They can be spilled before they are eventually popped.
            // Track them.
            PossibleSpilledValue(insn, basicValue.type).also { possibleSpilledValues += it }
        else basicValue.nonspillable()
    }

    override fun copyOperation(insn: AbstractInsnNode, value: BasicValue?): BasicValue? =
        when (value) {
            is ConstructedValue -> value
            is PossibleSpilledValue -> {
                value.usages += insn
                if (insn.opcode == Opcodes.ALOAD || insn.opcode == Opcodes.ASTORE) value
                else value.nonspillable()
            }
            else -> value?.nonspillable()
        }

    override fun naryOperation(insn: AbstractInsnNode, values: MutableList<out BasicValue?>): BasicValue? {
        for (value in values.filterIsInstance<PossibleSpilledValue>()) {
            value.usages += insn
        }
        return super.naryOperation(insn, values)?.nonspillable()
    }

    override fun merge(v: BasicValue?, w: BasicValue?): BasicValue? =
        if (v is PossibleSpilledValue && w is PossibleSpilledValue && v.source == w.source) v
        else v?.nonspillable()
}

// Inliner emits a lot of locals during inlining.
// Remove all of them since these locals are
//  1) going to be spilled into continuation object
//  2) breaking tail-call elimination
internal class RedundantLocalsEliminationMethodTransformer(private val suspensionPoints: List<SuspensionPoint>) : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val interpreter = RedundantSpillingInterpreter()
        val frames = MethodAnalyzer<BasicValue>(internalClassName, methodNode, interpreter).analyze()

        val toDelete = mutableSetOf<AbstractInsnNode>()
        for (spilledValue in interpreter.possibleSpilledValues.filter { it.usages.isNotEmpty() }) {
            @Suppress("UNCHECKED_CAST")
            val aloads = spilledValue.usages.filter { it.opcode == Opcodes.ALOAD } as List<VarInsnNode>

            if (aloads.isEmpty()) continue

            val slot = aloads.first().`var`

            if (aloads.any { it.`var` != slot }) continue
            for (aload in aloads) {
                methodNode.instructions.set(aload, spilledValue.source.clone())
            }

            toDelete.addAll(spilledValue.usages.filter { it.opcode == Opcodes.ASTORE })
            toDelete.add(spilledValue.source)
        }

        for (pop in methodNode.instructions.asSequence().filter { it.opcode == Opcodes.POP }) {
            val value = (frames[methodNode.instructions.indexOf(pop)]?.top() as? PossibleSpilledValue) ?: continue
            if (value.usages.isEmpty() && value.source !in suspensionPoints) {
                toDelete.add(pop)
                toDelete.add(value.source)
            }
        }

        // Remove unreachable instructions to simplify further analyses
        for (index in frames.indices) {
            if (frames[index] == null) {
                val insn = methodNode.instructions[index]
                if (insn !is LabelNode) {
                    toDelete.add(insn)
                }
            }
        }

        methodNode.instructions.removeAll(toDelete)
    }

    private fun AbstractInsnNode.clone() = when (this) {
        is FieldInsnNode -> FieldInsnNode(opcode, owner, name, desc)
        is VarInsnNode -> VarInsnNode(opcode, `var`)
        is InsnNode -> InsnNode(opcode)
        is TypeInsnNode -> TypeInsnNode(opcode, desc)
        else -> error("clone of $this is not implemented yet")
    }
}

// Handy debugging routing
@Suppress("unused")
fun MethodNode.nodeTextWithFrames(frames: Array<*>): String {
    var insns = nodeText.split("\n")
    val first = insns.indexOfLast { it.trim().startsWith("@") } + 1
    var last = insns.indexOfFirst { it.trim().startsWith("LOCALVARIABLE") }
    if (last < 0) last = insns.size
    val prefix = insns.subList(0, first).joinToString(separator = "\n")
    val postfix = insns.subList(last, insns.size).joinToString(separator = "\n")
    insns = insns.subList(first, last)
    if (insns.any { it.contains("TABLESWITCH") }) {
        var insideTableSwitch = false
        var buffer = ""
        val res = arrayListOf<String>()
        for (insn in insns) {
            if (insn.contains("TABLESWITCH")) {
                insideTableSwitch = true
            }
            if (insideTableSwitch) {
                buffer += insn
                if (insn.contains("default")) {
                    insideTableSwitch = false
                    res += buffer
                    buffer = ""
                    continue
                }
            } else {
                res += insn
            }
        }
        insns = res
    }
    return prefix + "\n" + insns.withIndex().joinToString(separator = "\n") { (index, insn) ->
        if (index >= frames.size) "N/A\t$insn" else "${frames[index]}\t$insn"
    } + "\n" + postfix
}
