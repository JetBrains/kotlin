/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen.optimization

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.codegen.optimization.common.ProperTrackedReferenceValue
import org.jetbrains.kotlin.codegen.optimization.common.ReferenceTrackingInterpreter
import org.jetbrains.kotlin.codegen.optimization.common.ReferenceValueDescriptor
import org.jetbrains.kotlin.codegen.optimization.common.TrackedReferenceValue
import org.jetbrains.kotlin.codegen.optimization.common.InsnSequence
import org.jetbrains.kotlin.codegen.optimization.common.removeEmptyCatchBlocks
import org.jetbrains.kotlin.codegen.optimization.common.removeUnusedLocalVariables
import org.jetbrains.kotlin.codegen.optimization.fixStack.peek
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame

class CapturedVarsOptimizationMethodTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        Transformer(internalClassName, methodNode).run()
    }

    // Tracks proper usages of objects corresponding to captured variables.
    //
    // The 'kotlin.jvm.internal.Ref.*' instance can be replaced with a local variable,
    // if all of the following conditions are satisfied:
    //  * It is created inside a current method.
    //  * The only permitted operations on it are:
    //      - store to a local variable
    //      - ALOAD, ASTORE
    //      - DUP, POP
    //      - GETFIELD <owner>.element, PUTFIELD <owner>.element
    //  * There's a corresponding local variable definition,
    //      and all ALOAD/ASTORE instructions operate on that particular local variable.
    //  * Its 'element' field is initialized at start of local variable visibility range.
    //
    // Note that for code that doesn't create Ref objects explicitly these conditions are true,
    // unless the Ref object escapes to a local class constructor (including local classes for lambdas).
    //
    private class CapturedVarDescriptor(val newInsn: TypeInsnNode, val refType: Type, val valueType: Type) : ReferenceValueDescriptor {
        var hazard = false

        var initCallInsn: MethodInsnNode? = null
        var localVar: LocalVariableNode? = null
        var localVarIndex = -1
        val astoreInsns: MutableCollection<VarInsnNode> = LinkedHashSet()
        val aloadInsns: MutableCollection<VarInsnNode> = LinkedHashSet()
        val stackInsns: MutableCollection<AbstractInsnNode> = LinkedHashSet()
        val getFieldInsns: MutableCollection<FieldInsnNode> = LinkedHashSet()
        val putFieldInsns: MutableCollection<FieldInsnNode> = LinkedHashSet()
        var cleanVarInstruction: VarInsnNode? = null

        fun canRewrite(): Boolean =
                !hazard &&
                initCallInsn != null &&
                localVar != null &&
                localVarIndex >= 0

        override fun onUseAsTainted() {
            hazard = true
        }
    }

    private class Transformer(private val internalClassName: String, private val methodNode: MethodNode) {
        private val refValues = ArrayList<CapturedVarDescriptor>()
        private val refValuesByNewInsn = LinkedHashMap<TypeInsnNode, CapturedVarDescriptor>()
        private val insns = methodNode.instructions.toArray()
        private lateinit var frames: Array<out Frame<BasicValue>?>

        val hasRewritableRefValues: Boolean
            get() = refValues.isNotEmpty()

        fun run() {
            createRefValues()
            if (!hasRewritableRefValues) return

            analyze()
            if (!hasRewritableRefValues) return

            rewrite()
        }

        private fun AbstractInsnNode.getIndex() = methodNode.instructions.indexOf(this)

        private fun createRefValues() {
            for (insn in insns) {
                if (insn.opcode == Opcodes.NEW && insn is TypeInsnNode) {
                    val type = Type.getObjectType(insn.desc)
                    if (AsmTypes.isSharedVarType(type)) {
                        val valueType = REF_TYPE_TO_ELEMENT_TYPE[type.internalName] ?: continue
                        val refValue = CapturedVarDescriptor(insn, type, valueType)
                        refValues.add(refValue)
                        refValuesByNewInsn[insn] = refValue
                    }
                }
            }
        }

        private inner class Interpreter : ReferenceTrackingInterpreter() {
            override fun newOperation(insn: AbstractInsnNode): BasicValue =
                    refValuesByNewInsn[insn]?.let { descriptor ->
                        ProperTrackedReferenceValue(descriptor.refType, descriptor)
                    }
                    ?: super.newOperation(insn)

            override fun processRefValueUsage(value: TrackedReferenceValue, insn: AbstractInsnNode, position: Int) {
                for (descriptor in value.descriptors) {
                    if (descriptor !is CapturedVarDescriptor) throw AssertionError("Unexpected descriptor: $descriptor")
                    when {
                        insn.opcode == Opcodes.ALOAD ->
                            descriptor.aloadInsns.add(insn as VarInsnNode)
                        insn.opcode == Opcodes.ASTORE ->
                            descriptor.astoreInsns.add(insn as VarInsnNode)
                        insn.opcode == Opcodes.GETFIELD && insn is FieldInsnNode && insn.name == REF_ELEMENT_FIELD && position == 0 ->
                            descriptor.getFieldInsns.add(insn)
                        insn.opcode == Opcodes.PUTFIELD && insn is FieldInsnNode && insn.name == REF_ELEMENT_FIELD && position == 0 ->
                            descriptor.putFieldInsns.add(insn)
                        insn.opcode == Opcodes.INVOKESPECIAL && insn is MethodInsnNode && insn.name == INIT_METHOD_NAME && position == 0 ->
                            if (descriptor.initCallInsn != null && descriptor.initCallInsn != insn)
                                descriptor.hazard = true
                            else
                                descriptor.initCallInsn = insn
                        insn.opcode == Opcodes.DUP ->
                            descriptor.stackInsns.add(insn)
                        else ->
                            descriptor.hazard = true
                    }
                }
            }

        }

        private fun analyze() {
            frames = MethodTransformer.analyze(internalClassName, methodNode, Interpreter())
            trackPops()
            assignLocalVars()

            refValues.removeAll { !it.canRewrite() }
        }

        private fun trackPops() {
            for (i in insns.indices) {
                val frame = frames[i] ?: continue
                val insn = insns[i]

                when (insn.opcode) {
                    Opcodes.POP -> {
                        frame.top()?.getCapturedVarOrNull()?.run { stackInsns.add(insn) }
                    }
                    Opcodes.POP2 -> {
                        val top = frame.top()
                        if (top?.size == 1) {
                            top.getCapturedVarOrNull()?.hazard = true
                            frame.peek(1)?.getCapturedVarOrNull()?.hazard = true
                        }
                    }
                }
            }
        }

        private fun BasicValue.getCapturedVarOrNull() =
                safeAs<ProperTrackedReferenceValue>()?.descriptor?.safeAs<CapturedVarDescriptor>()

        private fun assignLocalVars() {
            for (localVar in methodNode.localVariables) {
                val type = Type.getType(localVar.desc)
                if (!AsmTypes.isSharedVarType(type)) continue

                val startFrame = frames[localVar.start.getIndex()] ?: continue

                val refValue = startFrame.getLocal(localVar.index) as? ProperTrackedReferenceValue ?: continue
                val descriptor = refValue.descriptor as? CapturedVarDescriptor ?: continue

                if (descriptor.hazard) continue

                if (descriptor.localVar == null) {
                    descriptor.localVar = localVar
                }
                else {
                    descriptor.hazard = true
                }
            }

            for (refValue in refValues) {
                if (refValue.hazard) continue
                val localVar = refValue.localVar ?: continue
                val oldVarIndex = localVar.index

                if (refValue.valueType.size != 1) {
                    refValue.localVarIndex = methodNode.maxLocals
                    methodNode.maxLocals += 2
                    localVar.index = refValue.localVarIndex
                }
                else {
                    refValue.localVarIndex = localVar.index
                }

                val startIndex = localVar.start.getIndex()
                val initFieldInsns = refValue.putFieldInsns.filter { it.getIndex() < startIndex }
                if (initFieldInsns.size != 1) {
                    refValue.hazard = true
                    continue
                }

                val cleanInstructions = findCleanInstructions(refValue, oldVarIndex, methodNode.instructions)
                if (cleanInstructions.size > 1 ) {
                    refValue.hazard = true
                    continue
                }
                refValue.cleanVarInstruction = cleanInstructions.firstOrNull()
            }
        }

        private fun findCleanInstructions(refValue: CapturedVarDescriptor, oldVarIndex: Int, instructions: InsnList): List<VarInsnNode> {
            val cleanInstructions =
                InsnSequence(instructions).filterIsInstance<VarInsnNode>().filter {
                    it.opcode == Opcodes.ASTORE && it.`var` == oldVarIndex
                }.filter {
                    it.previous?.opcode == Opcodes.ACONST_NULL
                }.filter {
                    val operationIndex = instructions.indexOf(it)
                    val localVariableNode = refValue.localVar!!
                    instructions.indexOf(localVariableNode.start) < operationIndex && operationIndex < instructions.indexOf(localVariableNode.end)
                }.toList()
            return cleanInstructions
        }

        private fun rewrite() {
            for (refValue in refValues) {
                if (!refValue.canRewrite()) continue

                rewriteRefValue(refValue)
            }

            methodNode.removeEmptyCatchBlocks()
            methodNode.removeUnusedLocalVariables()
        }

        private fun rewriteRefValue(capturedVar: CapturedVarDescriptor) {
            methodNode.instructions.run {
                capturedVar.localVar!!.let {
                    it.signature = null
                    it.desc = capturedVar.valueType.descriptor
                }

                remove(capturedVar.newInsn)
                remove(capturedVar.initCallInsn!!)
                capturedVar.stackInsns.forEach { remove(it) }
                capturedVar.aloadInsns.forEach { remove(it) }
                capturedVar.astoreInsns.forEach { remove(it) }

                capturedVar.getFieldInsns.forEach {
                    set(it, VarInsnNode(capturedVar.valueType.getOpcode(Opcodes.ILOAD), capturedVar.localVarIndex))
                }

                capturedVar.putFieldInsns.forEach {
                    set(it, VarInsnNode(capturedVar.valueType.getOpcode(Opcodes.ISTORE), capturedVar.localVarIndex))
                }

                //after visiting block codegen tries to delete all allocated references:
                // see ExpressionCodegen.addLeaveTaskToRemoveLocalVariableFromFrameMap
                capturedVar.cleanVarInstruction?.let {
                    remove(it.previous)
                    remove(it)
                }
            }
        }
    }
}

internal const val REF_ELEMENT_FIELD = "element"
internal const val INIT_METHOD_NAME = "<init>"

internal val REF_TYPE_TO_ELEMENT_TYPE = HashMap<String, Type>().apply {
    put(AsmTypes.OBJECT_REF_TYPE.internalName, AsmTypes.OBJECT_TYPE)
    PrimitiveType.values().forEach {
        put(AsmTypes.sharedTypeForPrimitive(it).internalName, AsmTypes.valueTypeForPrimitive(it))
    }
}
