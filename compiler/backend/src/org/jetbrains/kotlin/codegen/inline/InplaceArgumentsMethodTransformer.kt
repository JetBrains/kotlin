/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode

class InplaceArgumentsMethodTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val methodContext = parseMethodOrNull(methodNode)
        if (methodContext != null) {
            if (methodContext.calls.isEmpty()) return
            collectStartToEnd(methodContext)
            transformMethod(methodContext)
            val stackSizeAfter = StackSizeCalculator(internalClassName, methodNode).calculateStackSize()
            methodNode.maxStack = stackSizeAfter
        }
        stripMarkers(methodNode)
    }

    private class MethodContext(
        val methodNode: MethodNode,
        val calls: List<CallContext>
    ) {
        val startArgToEndArg = HashMap<AbstractInsnNode, AbstractInsnNode>()
    }

    private class CallContext(
        val callStartMarker: AbstractInsnNode,
        val callEndMarker: AbstractInsnNode,
        val args: List<ArgContext>,
        val calls: List<CallContext>
    )

    private class ArgContext(
        val argStartMarker: AbstractInsnNode,
        val argEndMarker: AbstractInsnNode,
        val calls: List<CallContext>,
        val storeInsn: VarInsnNode
    ) {
        val loadOpcode = storeInsn.opcode - Opcodes.ISTORE + Opcodes.ILOAD

        val varIndex = storeInsn.`var`
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
                        calls.add(parseCall(insn, iter))
                    insn.isInplaceCallEndMarker() || insn.isInplaceArgumentStartMarker() || insn.isInplaceArgumentEndMarker() ->
                        throw ParseErrorException()
                }
            }
        } catch (e: ParseErrorException) {
            return null
        }
        return MethodContext(methodNode, calls)
    }

    private fun parseCall(start: AbstractInsnNode, iter: ListIterator<AbstractInsnNode>): CallContext {
        //  CALL    ::= callStartMarker insn* (ARG insn*)* (CALL insn*)* callEndMarker
        val args = ArrayList<ArgContext>()
        val calls = ArrayList<CallContext>()
        while (iter.hasNext()) {
            val insn = iter.next()
            when {
                insn.isInplaceCallStartMarker() ->
                    calls.add(parseCall(insn, iter))
                insn.isInplaceCallEndMarker() ->
                    return CallContext(start, insn, args, calls)
                insn.isInplaceArgumentStartMarker() ->
                    args.add(parseArg(insn, iter))
                insn.isInplaceArgumentEndMarker() ->
                    throw ParseErrorException()
            }
        }
        // Reached instruction list end, didn't find inplace-call-end marker
        throw ParseErrorException()
    }

    private fun parseArg(start: AbstractInsnNode, iter: ListIterator<AbstractInsnNode>): ArgContext {
        //  ARG     ::= argStartMarker insn* (CALL insn*)* argEndMarker storeInsn
        val calls = ArrayList<CallContext>()
        while (iter.hasNext()) {
            val insn = iter.next()
            when {
                insn.isInplaceCallStartMarker() ->
                    calls.add(parseCall(insn, iter))
                insn.isInplaceArgumentEndMarker() -> {
                    val next = insn.next
                    if (next is VarInsnNode && next.opcode in Opcodes.ISTORE..Opcodes.ASTORE) {
                        iter.next()
                        return ArgContext(start, insn, calls, next)
                    } else {
                        throw ParseErrorException()
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

    private fun transformMethod(methodContext: MethodContext) {
        for (call in methodContext.calls) {
            transformCall(methodContext, call)
        }
    }

    private fun transformCall(methodContext: MethodContext, callContext: CallContext) {
        for (arg in callContext.args) {
            transformArg(methodContext, arg)
        }

        for (call in callContext.calls) {
            transformCall(methodContext, call)
        }

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
                        // Replace argument load with argument body
                        var argInsn = arg.argStartMarker.next
                        while (argInsn != arg.argEndMarker) {
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

    private fun transformArg(methodContext: MethodContext, argContext: ArgContext) {
        // Transform nested calls inside argument
        for (call in argContext.calls) {
            transformCall(methodContext, call)
        }
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