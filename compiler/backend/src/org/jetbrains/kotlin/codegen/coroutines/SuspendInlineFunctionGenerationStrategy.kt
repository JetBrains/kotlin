/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.codegen.FunctionGenerationStrategy
import org.jetbrains.kotlin.codegen.TransformationMethodVisitor
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.codegen.inline.coroutines.findReceiverOfInvoke
import org.jetbrains.kotlin.codegen.inline.coroutines.surroundInvokesWithSuspendMarkers
import org.jetbrains.kotlin.codegen.inline.isInvokeOnLambda
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.fixStack.FixStackMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.JVMConstructorCallNormalizationMode
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceInterpreter

// For named suspend function we generate two methods:
// 1) to use as noinline function, which have state machine
// 2) to use from inliner: private one without state machine
class SuspendInlineFunctionGenerationStrategy(
    state: GenerationState,
    originalSuspendDescriptor: FunctionDescriptor,
    declaration: KtFunction,
    containingClassInternalName: String,
    constructorCallNormalizationMode: JVMConstructorCallNormalizationMode,
    codegen: FunctionCodegen
) : SuspendFunctionGenerationStrategy(
    state,
    originalSuspendDescriptor,
    declaration,
    containingClassInternalName,
    constructorCallNormalizationMode,
    codegen
) {
    private val defaultStrategy = FunctionGenerationStrategy.FunctionDefault(state, declaration)

    override fun wrapMethodVisitor(mv: MethodVisitor, access: Int, name: String, desc: String): MethodVisitor {
        if (access and Opcodes.ACC_ABSTRACT != 0) return mv

        return MethodNodeCopyingMethodVisitor(
            SurroundSuspendParameterCallsWithSuspendMarkersMethodVisitor(
                createStateMachineBuilder(mv, access, name, desc),
                access, name, desc, containingClassInternalName, originalSuspendDescriptor.valueParameters
            ), access, name, desc,
            newMethod = { origin, newAccess, newName, newDesc ->
                functionCodegen.newMethod(origin, newAccess, newName, newDesc, null, null)
            }, keepAccess = false
        )
    }

    override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
        super.doGenerateBody(codegen, signature)
        defaultStrategy.doGenerateBody(codegen, signature)
    }
}

class MethodNodeCopyingMethodVisitor(
    delegate: MethodVisitor, private val access: Int, private val name: String, private val desc: String,
    private val newMethod: (JvmDeclarationOrigin, Int, String, String) -> MethodVisitor,
    private val keepAccess: Boolean = true
) : TransformationMethodVisitor(
    delegate, calculateAccessForInline(access, keepAccess), "$name$FOR_INLINE_SUFFIX", desc, null, null
) {
    override fun performTransformations(methodNode: MethodNode) {
        val newMethodNode = newMethod(
            JvmDeclarationOrigin.NO_ORIGIN, calculateAccessForInline(access, keepAccess), "$name$FOR_INLINE_SUFFIX", desc
        )
        methodNode.instructions.resetLabels()
        methodNode.accept(newMethodNode)
    }

    companion object {
        private fun calculateAccessForInline(access: Int, keepAccess: Boolean): Int =
            if (keepAccess) access
            else access or Opcodes.ACC_PRIVATE and Opcodes.ACC_PUBLIC.inv() and Opcodes.ACC_PROTECTED.inv()
    }
}

private class SurroundSuspendParameterCallsWithSuspendMarkersMethodVisitor(
    delegate: MethodVisitor,
    access: Int,
    name: String,
    desc: String,
    private val thisName: String,
    private val valueParameters: List<ValueParameterDescriptor>
): TransformationMethodVisitor(delegate, access, name, desc, null, null) {
    override fun performTransformations(methodNode: MethodNode) {
        fun AbstractInsnNode.index() = methodNode.instructions.indexOf(this)
        fun AbstractInsnNode.isInlineSuspendParameter(): Boolean {
            if (this !is VarInsnNode) return false
            val index = `var` - (if (methodNode.access and Opcodes.ACC_STATIC != 0) 0 else 1)
            return opcode == Opcodes.ALOAD && index < valueParameters.size && InlineUtil.isInlineParameter(valueParameters[index]) &&
                    valueParameters[index].type.isSuspendFunctionType
        }

        FixStackMethodTransformer().transform(thisName, methodNode)

        val sourceFrames = MethodTransformer.analyze(thisName, methodNode, SourceInterpreter())

        val noinlineInvokes = arrayListOf<Pair<AbstractInsnNode, AbstractInsnNode>>()

        for (insn in methodNode.instructions.asSequence()) {
            if (insn.opcode != Opcodes.INVOKEINTERFACE) continue
            insn as MethodInsnNode
            if (!isInvokeOnLambda(insn.owner, insn.name)) continue
            val frame = sourceFrames[insn.index()] ?: continue
            val aload = findReceiverOfInvoke(frame, insn).takeIf { it?.isInlineSuspendParameter() == true } as? VarInsnNode ?: continue
            noinlineInvokes.add(insn to aload)
        }

        surroundInvokesWithSuspendMarkers(methodNode, noinlineInvokes)
    }
}