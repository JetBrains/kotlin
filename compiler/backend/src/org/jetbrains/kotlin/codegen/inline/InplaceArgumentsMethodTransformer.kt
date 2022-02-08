/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.optimization.boxing.isMethodInsnWith
import org.jetbrains.kotlin.codegen.optimization.common.*
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*

class InplaceArgumentsMethodTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val methodContext = parseMethodOrNull(methodNode)
        if (methodContext != null) {
            if (methodContext.calls.isEmpty()) return

            collectStartToEnd(methodContext)
            collectLvtEntryInstructions(methodContext)
            collectSuspensionPoints(methodContext)

            transformMethod(methodContext)

            methodNode.fixupLVT()

            methodNode.removeUnusedLocalVariables()
            methodNode.updateMaxStack()
        }
        stripMarkers(methodNode)
    }

    private class MethodContext(
        val methodNode: MethodNode,
        val calls: List<CallContext>
    ) {
        val startArgToEndArg = HashMap<AbstractInsnNode, AbstractInsnNode>()
        val lvtEntryForInstruction = HashMap<AbstractInsnNode, LocalVariableNode>()
        val varInstructionMoved = HashMap<AbstractInsnNode, CallContext>()
        val suspensionJumpLabels = HashSet<LabelNode>()
    }

    private class CallContext(
        val callStartMarker: AbstractInsnNode,
        val callEndMarker: AbstractInsnNode,
        val args: List<ArgContext>,
        val calls: List<CallContext>,
    )

    private class ArgContext(
        val argStartMarker: AbstractInsnNode,
        val argEndMarker: AbstractInsnNode,
        val calls: List<CallContext>,
        val storeInsn: VarInsnNode?
    ) {
        val loadOpcode =
            if (storeInsn != null)
                storeInsn.opcode - Opcodes.ISTORE + Opcodes.ILOAD
            else
                -1

        val varIndex = storeInsn?.`var` ?: -1
    }

    private fun parseMethodOrNull(methodNode: MethodNode): MethodContext? {
        // We assume that the method body structure follows this grammar:
        //  METHOD  ::= insn* (CALL insn*)*
        //  CALL    ::= callStartMarker insn* (ARG insn*)* (CALL insn*)* callEndMarker
        //  ARG     ::= argStartMarker insn* (CALL insn*)* argEndMarker storeInsn

        val iter = methodNode.instructions.iterator()
        val calls = ArrayList<CallContext>()
        try {
            while (iter.hasNext()) {
                val insn = iter.next()
                when {
                    insn.isInplaceCallStartMarker() ->
                        calls.add(parseCall(methodNode, insn, iter))
                    insn.isInplaceCallEndMarker() || insn.isInplaceArgumentStartMarker() || insn.isInplaceArgumentEndMarker() ->
                        throw ParseErrorException()
                }
            }
        } catch (e: ParseErrorException) {
            return null
        }
        return MethodContext(methodNode, calls)
    }

    private fun parseCall(methodNode: MethodNode, start: AbstractInsnNode, iter: ListIterator<AbstractInsnNode>): CallContext {
        //  CALL    ::= callStartMarker insn* (ARG insn*)* (CALL insn*)* callEndMarker
        val args = ArrayList<ArgContext>()
        val calls = ArrayList<CallContext>()
        while (iter.hasNext()) {
            val insn = iter.next()
            when {
                insn.isInplaceCallStartMarker() ->
                    calls.add(parseCall(methodNode, insn, iter))
                insn.isInplaceCallEndMarker() ->
                    return CallContext(start, insn, args, calls)
                insn.isInplaceArgumentStartMarker() ->
                    args.add(parseArg(methodNode, insn, iter))
                insn.isInplaceArgumentEndMarker() ->
                    throw ParseErrorException()
            }
        }
        // Reached instruction list end, didn't find inplace-call-end marker
        throw ParseErrorException()
    }

    private fun parseArg(methodNode: MethodNode, start: AbstractInsnNode, iter: ListIterator<AbstractInsnNode>): ArgContext {
        //  ARG     ::= argStartMarker insn* (CALL insn*)* argEndMarker storeInsn
        val calls = ArrayList<CallContext>()
        while (iter.hasNext()) {
            val insn = iter.next()
            when {
                insn.isInplaceCallStartMarker() ->
                    calls.add(parseCall(methodNode, insn, iter))
                insn.isInplaceArgumentEndMarker() -> {
                    val next = insn.next
                    return when {
                        next is VarInsnNode && next.opcode in Opcodes.ISTORE..Opcodes.ASTORE -> {
                            iter.next()
                            ArgContext(start, insn, calls, next)
                        }
                        insn.previous.isInplaceArgumentStartMarker() -> {
                            // Empty argument - a remapped variable.
                            // For such argument varIndex would be '-1', so it would not be associated with any LOAD instruction.
                            ArgContext(start, insn, calls, null)
                        }
                        else -> {
                            throw ParseErrorException()
                        }
                    }
                }
                insn.isInplaceCallEndMarker() || insn.isInplaceArgumentStartMarker() ->
                    throw ParseErrorException()
            }
        }
        // Reached instruction list end, didn't find inplace-argument-end marker
        throw ParseErrorException()
    }

    private class ParseErrorException : RuntimeException() {
        override fun fillInStackTrace(): Throwable = this
    }

    private fun collectStartToEnd(methodContext: MethodContext) {
        for (call in methodContext.calls) {
            collectStartToEnd(methodContext, call)
        }
    }

    private fun collectStartToEnd(methodContext: MethodContext, callContext: CallContext) {
        for (arg in callContext.args) {
            collectStartToEnd(methodContext, arg)
        }
        for (call in callContext.calls) {
            collectStartToEnd(methodContext, call)
        }
    }

    private fun collectStartToEnd(methodContext: MethodContext, argContext: ArgContext) {
        methodContext.startArgToEndArg[argContext.argStartMarker] = argContext.argEndMarker
        for (call in argContext.calls) {
            collectStartToEnd(methodContext, call)
        }
    }

    private fun collectLvtEntryInstructions(methodContext: MethodContext) {
        val insnList = methodContext.methodNode.instructions
        val insnArray = insnList.toArray()
        for (lv in methodContext.methodNode.localVariables) {
            val lvStartIndex = insnList.indexOf(lv.start)
            val lvEndIndex = insnList.indexOf(lv.end)
            for (i in lvStartIndex until lvEndIndex) {
                val insn = insnArray[i]
                if (insn.opcode in Opcodes.ILOAD..Opcodes.ALOAD || insn.opcode in Opcodes.ISTORE..Opcodes.ASTORE) {
                    if ((insn as VarInsnNode).`var` == lv.index) {
                        methodContext.lvtEntryForInstruction[insn] = lv
                    }
                } else if (insn.opcode == Opcodes.IINC) {
                    if ((insn as IincInsnNode).`var` == lv.index) {
                        methodContext.lvtEntryForInstruction[insn] = lv
                    }
                }
            }
        }
    }

    private fun collectSuspensionPoints(methodContext: MethodContext) {
        val insnList = methodContext.methodNode.instructions
        var insn = insnList.first
        while (
            !insn.isMethodInsnWith(Opcodes.INVOKESTATIC) {
                owner == "kotlin/coroutines/intrinsics/IntrinsicsKt" &&
                        name == "getCOROUTINE_SUSPENDED" &&
                        desc == "()Ljava/lang/Object;"
            }
        ) {
            insn = insn.next ?: return
        }

        // Find a first TABLESWITCH and record its jump destinations
        while (insn != null) {
            if (insn.opcode != Opcodes.TABLESWITCH || insn.previous.opcode != Opcodes.GETFIELD) {
                insn = insn.next
                continue
            }
            val getFiendInsn = insn.previous as FieldInsnNode
            if (getFiendInsn.name != "label" || getFiendInsn.desc != "I") {
                insn = insn.next
                continue
            }
            val tableSwitchInsn = insn as TableSwitchInsnNode
            methodContext.suspensionJumpLabels.addAll(tableSwitchInsn.labels)
            methodContext.suspensionJumpLabels.add(tableSwitchInsn.dflt)
            return
        }
    }

    private fun transformMethod(methodContext: MethodContext) {
        for (call in methodContext.calls) {
            transformCall(methodContext, call)
        }
    }

    private fun transformCall(methodContext: MethodContext, callContext: CallContext) {
        // Transform nested calls
        for (arg in callContext.args) {
            for (nestedCall in arg.calls) {
                transformCall(methodContext, nestedCall)
            }
        }
        for (call in callContext.calls) {
            transformCall(methodContext, call)
        }

        if (callContext.args.any { it.isUnsafeToMove(methodContext) }) {
            // Do not transform such call, just strip call and argument markers.
            val insnList = methodContext.methodNode.instructions
            for (arg in callContext.args) {
                insnList.remove(arg.argStartMarker)
                insnList.remove(arg.argEndMarker)
            }
            insnList.remove(callContext.callStartMarker)
            insnList.remove(callContext.callEndMarker)
            return
        }

        moveInplaceArgumentsFromStoresToLoads(methodContext, callContext)
    }

    private fun ArgContext.isUnsafeToMove(methodContext: MethodContext): Boolean {
        // The following operations make inplace argument unsafe to move:
        // - non-local jump (moving such argument inside inline function body can interfere with stack normalization);
        // - variable store (variables defined inside argument can interfere with variables in inline function body).
        // TODO investigate whether it's possible to lift these restrictions.
        val argInsns = InsnSequence(this.argStartMarker, this.argEndMarker)
        val localLabels = argInsns.filterTo(HashSet()) { it is LabelNode }
        return argInsns.any { insn ->
            insn.isStoreOperation() ||
                    insn in methodContext.suspensionJumpLabels ||
                    insn.opcode == Opcodes.GOTO && (insn as JumpInsnNode).label !in localLabels
        }
    }

    private fun moveInplaceArgumentsFromStoresToLoads(methodContext: MethodContext, callContext: CallContext) {
        // Transform call
        val insnList = methodContext.methodNode.instructions
        val args = callContext.args.associateBy { it.varIndex }
        var argsProcessed = 0

        var insn: AbstractInsnNode = callContext.callStartMarker
        while (insn != callContext.callEndMarker) {
            when {
                insn.isInplaceArgumentStartMarker() -> {
                    // Skip argument body
                    insn = methodContext.startArgToEndArg[insn]!!
                }

                insn.opcode in Opcodes.ILOAD..Opcodes.ALOAD -> {
                    // Load instruction
                    val loadInsn = insn as VarInsnNode
                    val varIndex = loadInsn.`var`
                    val arg = args[varIndex]

                    if (arg == null || arg.loadOpcode != insn.opcode) {
                        // Not an argument load
                        insn = insn.next
                    } else {
                        // For each argument within this call we have
                        //      <inplaceArgStartMarker>
                        //      <argumentBody>
                        //      <inplaceArgEndMarker>
                        //      store [arg]
                        //      ...
                        //      load [arg]
                        // Replace 'load [arg]' with '<argumentBody>', drop 'store [arg]' and argument markers.

                        var argInsn = arg.argStartMarker.next
                        while (argInsn != arg.argEndMarker) {
                            // If a LOAD/STORE/IINC instruction was moved,
                            // record it so that we can update corresponding LVT entry if needed.
                            // NB it's better to do so after all transformations, so that we don't recalculate node indices.
                            if (argInsn.opcode in Opcodes.ILOAD..Opcodes.ALOAD ||
                                argInsn.opcode in Opcodes.ISTORE..Opcodes.ASTORE ||
                                argInsn.opcode == Opcodes.IINC
                            ) {
                                methodContext.varInstructionMoved[argInsn] = callContext
                            }

                            val argInsnNext = argInsn.next
                            insnList.remove(argInsn)
                            insnList.insertBefore(loadInsn, argInsn)
                            argInsn = argInsnNext
                        }

                        // Remove argument load and corresponding argument store instructions
                        insnList.remove(arg.storeInsn)
                        insn = loadInsn.next
                        insnList.remove(loadInsn)

                        // Replace subsequent argument loads with DUP instructions of appropriate size
                        while (insn.opcode == loadInsn.opcode && (insn as VarInsnNode).`var` == varIndex) {
                            if (insn.opcode == Opcodes.LLOAD || insn.opcode == Opcodes.DLOAD) {
                                insnList.insertBefore(insn, InsnNode(Opcodes.DUP2))
                            } else {
                                insnList.insertBefore(insn, InsnNode(Opcodes.DUP))
                            }
                            val next = insn.next
                            insnList.remove(insn)
                            insn = next
                        }

                        // Remove argument markers
                        insnList.remove(arg.argStartMarker)
                        insnList.remove(arg.argEndMarker)

                        // If there are no more inplace arguments left to process, we are done
                        ++argsProcessed
                        if (argsProcessed >= callContext.args.size)
                            break
                    }
                }

                else ->
                    insn = insn.next
            }
        }

        // Remove call start and call end markers
        insnList.remove(callContext.callStartMarker)
        insnList.remove(callContext.callEndMarker)
    }

    private fun stripMarkers(methodNode: MethodNode) {
        var insn = methodNode.instructions.first
        while (insn != null) {
            if (insn.isInplaceCallStartMarker() ||
                insn.isInplaceCallEndMarker() ||
                insn.isInplaceArgumentStartMarker() ||
                insn.isInplaceArgumentEndMarker()
            ) {
                val next = insn.next
                methodNode.instructions.remove(insn)
                insn = next
                continue
            }
            insn = insn.next
        }
    }
}

// HACK: if new end label is before start label, change to the next one
private fun MethodNode.fixupLVT() {
    for (localVariable in localVariables) {
        val startIndex = instructions.indexOf(localVariable.start)
        val endIndex = instructions.indexOf(localVariable.end)
        if (endIndex < startIndex) {
            val newEnd = localVariable.start.findNextOrNull { it is LabelNode } as? LabelNode
            localVariable.end = newEnd ?: localVariable.start
        }
    }
}