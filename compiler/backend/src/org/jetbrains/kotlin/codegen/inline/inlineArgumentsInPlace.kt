/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.optimization.nullCheck.isParameterCheckedForNull
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.*


fun canInlineArgumentsInPlace(methodNode: MethodNode): Boolean {
    // Usual inline functions are inlined in the following way:
    //      <evaluate argument #1>
    //      <store argument to an argument variable V1>
    //      ...
    //      <evaluate argument #N>
    //      <store argument to an argument variable VN>
    //      <inline function method body with parameter variables Pi remapped to argument variables Vi>
    // If an argument #k is already stored in a local variable W, this variable W is reused.
    // When inlining arguments in-place, we instead replace corresponding variable load instructions in the inline function method body
    // with bytecode for evaluating a given argument.
    // We can do so if such transformation keeps the evaluation order intact, possibly disregarding class initialization.

    val tcbStartLabels = methodNode.tryCatchBlocks.mapTo(HashSet()) { it.start }

    val methodParameterTypes = Type.getArgumentTypes(methodNode.desc)

    val jvmArgumentTypes = ArrayList<Type>(methodParameterTypes.size + 1)
    if (methodNode.access and Opcodes.ACC_STATIC == 0) {
        // Here we don't care much about the exact 'this' type,
        // it's only important to remember that variable slot #0 holds an object reference.
        jvmArgumentTypes.add(AsmTypes.OBJECT_TYPE)
    }
    jvmArgumentTypes.addAll(methodParameterTypes)

    val argumentVarEnd = jvmArgumentTypes.sumOf { it.size }
    var expectedArgumentVar = 0
    var lastArgIndex = 0

    var insn = methodNode.instructions.first

    // During arguments evaluation, make sure that all arguments are loaded in expected order
    // and there are no unexpected side effects in-between.
    while (insn != null && expectedArgumentVar < argumentVarEnd) {
        // Entering a try-catch block before all arguments are loaded breaks evaluation order.
        if (insn in tcbStartLabels)
            return false

        // Some instructions break evaluation order.
        if (insn.isProhibitedDuringArgumentsEvaluation())
            return false

        // Allow a limited list of 'GETSTATIC <owner> <name> <desc>' instructions.
        if (insn.opcode == Opcodes.GETSTATIC) {
            val fieldInsn = insn as FieldInsnNode
            val fieldSignature = FieldSignature(fieldInsn.owner, fieldInsn.name, fieldInsn.desc)
            if (fieldSignature !in whitelistedStaticFields)
                return false
        }

        // Writing to or incrementing an argument variable forbids in-place argument inlining.
        if (insn.opcode in Opcodes.ISTORE..Opcodes.ASTORE && (insn as VarInsnNode).`var` < argumentVarEnd)
            return false
        if (insn.opcode == Opcodes.IINC && (insn as IincInsnNode).`var` < argumentVarEnd)
            return false

        // Analyze variable loads.
        if (insn.opcode in Opcodes.ILOAD..Opcodes.ALOAD) {
            // Skip parameter null check: 'aload x; ldc "..."; invokestatic <check>'
            if (insn.opcode == Opcodes.ALOAD && insn.isParameterCheckedForNull()) {
                // Go directly to the instruction after 'invokestatic <check>'
                insn = insn.next.next.next
                continue
            }

            val varInsn = insn as VarInsnNode
            val varIndex = (varInsn).`var`
            if (varIndex == expectedArgumentVar) {
                // Expected argument variable loaded.
                expectedArgumentVar += jvmArgumentTypes[lastArgIndex].size
                ++lastArgIndex
                // Skip a sequence of load instructions referring to the same argument variable
                // (such sequence is present in functions like 'Array.copyOf' and can be replaced with DUP instructions).
                do {
                    insn = insn.next
                } while (insn != null && insn.opcode == varInsn.opcode && (insn as VarInsnNode).`var` == varIndex)
                continue
            } else if (varIndex < argumentVarEnd) {
                // Loaded an argument variable, but not an expected one => broken evaluation order
                return false
            } else {
                // It's OK to load any non-argument variable during argument evaluation.
                insn = insn.next
                continue
            }
        }

        // Anything else is fine.
        insn = insn.next
    }

    // Method body is over, but not all arguments were loaded on stack.
    if (expectedArgumentVar < argumentVarEnd)
        return false

    // After arguments evaluation make sure that argument variables are no longer accessed
    // (we are not going to store anything to those variables anyway).
    while (insn != null) {
        if (insn.opcode in Opcodes.ILOAD..Opcodes.ALOAD || insn.opcode in Opcodes.ISTORE..Opcodes.ASTORE) {
            if ((insn as VarInsnNode).`var` < argumentVarEnd)
                return false
        } else if (insn.opcode == Opcodes.IINC) {
            if ((insn as IincInsnNode).`var` < argumentVarEnd)
                return false
        }
        insn = insn.next
    }

    // Didn't encounter anything suspicious.
    return true
}

internal data class FieldSignature(
    val owner: String,
    val name: String,
    val desc: String
)

private val whitelistedStaticFields: Set<FieldSignature> =
    hashSetOf(
        FieldSignature("kotlin/Result", "Companion", "Lkotlin/Result\$Companion;"),
        FieldSignature("kotlin/_Assertions", "ENABLED", "Z")
    )

private fun AbstractInsnNode.isProhibitedDuringArgumentsEvaluation() =
    opcode in opcodeProhibitedDuringArgumentsEvaluation.indices &&
            opcodeProhibitedDuringArgumentsEvaluation[opcode]

private val opcodeProhibitedDuringArgumentsEvaluation = BooleanArray(256).also { a ->
    // Any kind of jump during arguments evaluation is a hazard.
    // This includes all conditional jump instructions, switch instructions, return and throw instructions.
    // Very conservative, but enough for practical cases.
    for (i in Opcodes.IFEQ..Opcodes.RETURN) a[i] = true
    a[Opcodes.IFNULL] = true
    a[Opcodes.IFNONNULL] = true
    a[Opcodes.ATHROW] = true

    // Instruction with non-trivial side effects is a hazard.
    // NB GETSTATIC is taken care of separately.
    a[Opcodes.PUTSTATIC] = true
    a[Opcodes.PUTFIELD] = true
    a[Opcodes.INVOKEVIRTUAL] = true
    a[Opcodes.INVOKESPECIAL] = true
    a[Opcodes.INVOKESTATIC] = true
    a[Opcodes.INVOKEINTERFACE] = true
    a[Opcodes.INVOKEDYNAMIC] = true
    a[Opcodes.MONITORENTER] = true
    a[Opcodes.MONITOREXIT] = true

    // Integer division instructions can throw exception
    a[Opcodes.IDIV] = true
    a[Opcodes.LDIV] = true
    a[Opcodes.IREM] = true
    a[Opcodes.LREM] = true

    // CHECKCAST can throw exception
    a[Opcodes.CHECKCAST] = true

    // Array creation can throw exception (in case of negative array size)
    a[Opcodes.NEWARRAY] = true
    a[Opcodes.ANEWARRAY] = true
    a[Opcodes.MULTIANEWARRAY] = true

    // Array access instructions can throw exception
    for (i in Opcodes.IALOAD..Opcodes.SALOAD) a[i] = true
    for (i in Opcodes.IASTORE..Opcodes.SASTORE) a[i] = true
}


private const val MARKER_INPLACE_CALL_START = "<INPLACE-CALL-START>"
private const val MARKER_INPLACE_ARGUMENT_START = "<INPLACE-ARGUMENT-START>"
private const val MARKER_INPLACE_ARGUMENT_END = "<INPLACE-ARGUMENT-END>"
private const val MARKER_INPLACE_CALL_END = "<INPLACE-CALL-END>"


private fun InstructionAdapter.addMarker(name: String) {
    visitMethodInsn(Opcodes.INVOKESTATIC, INLINE_MARKER_CLASS_NAME, name, "()V", false)
}

fun InstructionAdapter.addInplaceCallStartMarker() = addMarker(MARKER_INPLACE_CALL_START)
fun InstructionAdapter.addInplaceCallEndMarker() = addMarker(MARKER_INPLACE_CALL_END)
fun InstructionAdapter.addInplaceArgumentStartMarker() = addMarker(MARKER_INPLACE_ARGUMENT_START)
fun InstructionAdapter.addInplaceArgumentEndMarker() = addMarker(MARKER_INPLACE_ARGUMENT_END)

internal fun AbstractInsnNode.isInplaceCallStartMarker() = isInlineMarker(this, MARKER_INPLACE_CALL_START)
internal fun AbstractInsnNode.isInplaceCallEndMarker() = isInlineMarker(this, MARKER_INPLACE_CALL_END)
internal fun AbstractInsnNode.isInplaceArgumentStartMarker() = isInlineMarker(this, MARKER_INPLACE_ARGUMENT_START)
internal fun AbstractInsnNode.isInplaceArgumentEndMarker() = isInlineMarker(this, MARKER_INPLACE_ARGUMENT_END)
