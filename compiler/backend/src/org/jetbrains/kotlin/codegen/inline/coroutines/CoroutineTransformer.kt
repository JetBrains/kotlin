/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline.coroutines

import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.coroutines.*
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicInterpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame

const val FOR_INLINE_SUFFIX = "\$\$forInline"

class CoroutineTransformer(
    private val inliningContext: InliningContext,
    private val classBuilder: ClassBuilder,
    private val methods: List<MethodNode>,
    private val superClassName: String
) {
    private val state = inliningContext.state

    // If we inline into inline function, we should generate both method with state-machine for Java interop and method without
    // state-machine for further transformation/inlining.
    private val generateForInline = inliningContext.callSiteInfo.isInlineOrInsideInline

    fun shouldSkip(node: MethodNode): Boolean = methods.any { it.name == node.name + FOR_INLINE_SUFFIX && it.desc == node.desc }

    fun shouldGenerateStateMachine(node: MethodNode): Boolean {
        // Continuations are similar to lambdas from bird's view, but we should never generate state machine for them
        if (isContinuationNotLambda()) return false
        return isSuspendFunctionWithFakeConstructorCall(node) || (isSuspendLambda(node) && !isStateMachine(node))
    }

    // there can be suspend lambdas inside inline functions, which do not
    // capture crossinline lambdas, thus, there is no need to transform them
    fun suspendLambdaWithGeneratedStateMachine(node: MethodNode): Boolean =
        !isContinuationNotLambda() && isSuspendLambda(node) && isStateMachine(node)

    private fun isContinuationNotLambda(): Boolean = inliningContext.isContinuation && superClassName.endsWith("ContinuationImpl")

    private fun isStateMachine(node: MethodNode): Boolean =
        node.instructions.asSequence().any { insn -> insn is LdcInsnNode && insn.cst == ILLEGAL_STATE_ERROR_MESSAGE }

    private fun isSuspendLambda(node: MethodNode) = isInvokeSuspend(node)

    fun newMethod(node: MethodNode): DeferredMethodVisitor {
        return when {
            isInvokeSuspend(node) -> {
                assert(!isStateMachine(node)) {
                    "Inlining/transforming state-machine"
                }
                newStateMachineForLambda(node)
            }
            isSuspendFunctionWithFakeConstructorCall(node) -> newStateMachineForNamedFunction(node)
            else -> error("no need to generate state maching for ${node.name}")
        }
    }

    private fun isInvokeSuspend(node: MethodNode): Boolean =
        node.name.removeSuffix(FOR_INLINE_SUFFIX) == INVOKE_SUSPEND_METHOD_NAME && inliningContext.isContinuation

    private fun isSuspendFunctionWithFakeConstructorCall(node: MethodNode): Boolean = findFakeContinuationConstructorClassName(node) != null

    private fun newStateMachineForLambda(node: MethodNode): DeferredMethodVisitor {
        val name = node.name.removeSuffix(FOR_INLINE_SUFFIX)
        return DeferredMethodVisitor(
            MethodNode(
                node.access, name, node.desc, node.signature,
                ArrayUtil.toStringArray(node.exceptions)
            )
        ) {
            val sourceCompilerForInline = inliningContext.root.sourceCompilerForInline
            val stateMachineBuilder = CoroutineTransformerMethodVisitor(
                createNewMethodFrom(node, name), node.access, name, node.desc, null, null,
                containingClassInternalName = classBuilder.thisName,
                obtainClassBuilderForCoroutineState = { classBuilder },
                isForNamedFunction = false,
                disableTailCallOptimizationForFunctionReturningUnit = false,
                reportSuspensionPointInsideMonitor = { sourceCompilerForInline.reportSuspensionPointInsideMonitor(it) },
                // TODO: this linenumbers might not be correct and since they are used only for step-over, check them.
                lineNumber = inliningContext.callSiteInfo.lineNumber,
                sourceFile = inliningContext.callSiteInfo.file?.name ?: "",
            )

            if (generateForInline)
                SuspendForInlineCopyingMethodVisitor(stateMachineBuilder, node.access, name, node.desc, classBuilder::newMethod)
            else
                stateMachineBuilder
        }
    }

    private fun newStateMachineForNamedFunction(node: MethodNode): DeferredMethodVisitor {
        val name = node.name.removeSuffix(FOR_INLINE_SUFFIX)
        val continuationClassName = findFakeContinuationConstructorClassName(node)
        assert(inliningContext is RegeneratedClassContext)
        return DeferredMethodVisitor(
            MethodNode(
                node.access, name, node.desc, node.signature,
                ArrayUtil.toStringArray(node.exceptions)
            )
        ) {
            // If the node already has state-machine, it is safer to generate state-machine.
            val disableTailCallOptimization = methods.find { it.name == name && it.desc == node.desc }?.let { isStateMachine(it) } ?: false
            val sourceCompilerForInline = inliningContext.root.sourceCompilerForInline
            val stateMachineBuilder = CoroutineTransformerMethodVisitor(
                createNewMethodFrom(node, name), node.access, name, node.desc, null, null,
                containingClassInternalName = classBuilder.thisName,
                obtainClassBuilderForCoroutineState = { (inliningContext as RegeneratedClassContext).continuationBuilders[continuationClassName]!! },
                isForNamedFunction = true,
                disableTailCallOptimizationForFunctionReturningUnit = disableTailCallOptimization,
                reportSuspensionPointInsideMonitor = { sourceCompilerForInline.reportSuspensionPointInsideMonitor(it) },
                lineNumber = inliningContext.callSiteInfo.lineNumber,
                sourceFile = inliningContext.callSiteInfo.file?.name ?: "",
                needDispatchReceiver = true,
                internalNameForDispatchReceiver = classBuilder.thisName,
                putContinuationParameterToLvt = !state.isIrBackend,
            )

            if (generateForInline)
                SuspendForInlineCopyingMethodVisitor(stateMachineBuilder, node.access, name, node.desc, classBuilder::newMethod)
            else
                stateMachineBuilder
        }
    }

    private fun createNewMethodFrom(node: MethodNode, name: String): MethodVisitor {
        return classBuilder.newMethod(
            JvmDeclarationOrigin.NO_ORIGIN, node.access, name, node.desc, node.signature, ArrayUtil.toStringArray(node.exceptions)
        )
    }

    fun replaceFakesWithReals(node: MethodNode) {
        findFakeContinuationConstructorClassName(node)?.let(::unregisterClassBuilder)?.done(state.config.generateSmapCopyToAnnotation)
        replaceFakeContinuationsWithRealOnes(
            node, if (!inliningContext.isContinuation) getLastParameterIndex(node.desc, node.access) else 0
        )
    }

    fun registerClassBuilder(continuationClassName: String) {
        val context = inliningContext.parent?.parent as? RegeneratedClassContext ?: error("incorrect context")
        context.continuationBuilders[continuationClassName] = classBuilder
    }

    private fun unregisterClassBuilder(continuationClassName: String): ClassBuilder? =
        (inliningContext as RegeneratedClassContext).continuationBuilders.remove(continuationClassName)

    // If tail-call optimization took place, we do not need continuation class anymore, unless it is used by $$forInline method
    fun safeToRemoveContinuationClass(method: MethodNode): Boolean = !generateForInline && !isStateMachine(method)

    fun oldContinuationFrom(method: MethodNode): String? =
        methods.find { it.name == method.name + FOR_INLINE_SUFFIX && it.desc == method.desc }
            ?.let { findFakeContinuationConstructorClassName(it) }

    companion object {
        fun findFakeContinuationConstructorClassName(node: MethodNode): String? {
            val marker = node.instructions.asSequence().firstOrNull(::isBeforeFakeContinuationConstructorCallMarker) ?: return null
            val new = marker.next
            assert(new?.opcode == Opcodes.NEW)
            return (new as TypeInsnNode).desc
        }
    }
}

private const val NOINLINE_CALL_MARKER = "\$\$\$\$\$NOINLINE_CALL_MARKER\$\$\$\$\$"

fun markNoinlineLambdaIfSuspend(mv: MethodVisitor, info: FunctionalArgument?) {
    when (info) {
        NonInlineArgumentForInlineSuspendParameter.OTHER ->
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, NOINLINE_CALL_MARKER, "always", "()V", false)
        NonInlineArgumentForInlineSuspendParameter.INLINE_LAMBDA_AS_VARIABLE -> // depends on how it's substituted later
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, NOINLINE_CALL_MARKER, "conditional", "()V", false)
    }
}

private fun Frame<BasicValue>.getSource(offset: Int): AbstractInsnNode? = (getStack(stackSize - offset - 1) as? PossibleLambdaLoad)?.insn

fun surroundInvokesWithSuspendMarkersIfNeeded(node: MethodNode) {
    val markers = node.instructions.asSequence().filter {
        it.opcode == Opcodes.INVOKESTATIC && (it as MethodInsnNode).owner == NOINLINE_CALL_MARKER
    }.toList()
    if (markers.isEmpty()) return

    val sourceFrames = MethodTransformer.analyze("fake", node, CapturedLambdaInterpreter())
    val loads = markers.map { marker ->
        val arity = (marker.next as MethodInsnNode).owner.removePrefix(NUMBERED_FUNCTION_PREFIX).toInt()
        var receiver = sourceFrames[node.instructions.indexOf(marker) + 1]?.getSource(arity)
        // Navigate the ALOAD+GETFIELD+... chain to the first instruction. We need to insert a stack
        // spilling marker before it starts.
        while (receiver?.opcode == Opcodes.GETFIELD) {
            receiver = receiver.previous
        }
        receiver
    }
    for ((marker, load) in markers.zip(loads)) {
        val conditional = (marker as MethodInsnNode).name == "conditional"
        val invoke = marker.next as MethodInsnNode
        node.instructions.remove(marker)
        if (load == null) {
            continue // dead code, doesn't matter
        }
        node.instructions.insertBefore(load, withInstructionAdapter {
            addInlineMarker(this, isStartNotEnd = true)
        })
        node.instructions.insertBefore(invoke, withInstructionAdapter {
            addSuspendMarker(this, isStartNotEnd = true, inlinable = conditional)
        })
        node.instructions.insert(invoke, withInstructionAdapter {
            addSuspendMarker(this, isStartNotEnd = false, inlinable = conditional)
            addInlineMarker(this, isStartNotEnd = false)
        })
    }
}

// Interpreter, that keeps track of captured functional arguments
private class PossibleLambdaLoad(val insn: AbstractInsnNode) : BasicValue(AsmTypes.OBJECT_TYPE)

private class CapturedLambdaInterpreter : BasicInterpreter(Opcodes.API_VERSION) {
    override fun newOperation(insn: AbstractInsnNode): BasicValue? {
        if (insn.opcode == Opcodes.GETSTATIC) {
            insn.fieldLoad()?.let { return it }
        }

        return super.newOperation(insn)
    }

    private fun AbstractInsnNode.fieldLoad(): PossibleLambdaLoad? {
        if (this !is FieldInsnNode) return null
        if (desc.startsWith('L') && Type.getType(desc).internalName.isNumberedFunctionInternalName()) {
            if ((opcode == Opcodes.GETSTATIC && name.startsWith(CAPTURED_FIELD_FOLD_PREFIX + CAPTURED_FIELD_PREFIX)) ||
                (opcode == Opcodes.GETFIELD && isCapturedFieldName(name))
            ) return PossibleLambdaLoad(this)
        }
        return null
    }

    override fun copyOperation(insn: AbstractInsnNode, value: BasicValue?): BasicValue? =
        if (insn.opcode == Opcodes.ALOAD) PossibleLambdaLoad(insn) else super.copyOperation(insn, value)

    override fun unaryOperation(insn: AbstractInsnNode, value: BasicValue?): BasicValue? {
        if (insn.opcode == Opcodes.GETFIELD) {
            insn.fieldLoad()?.let { return it }
        }
        return super.unaryOperation(insn, value)
    }

    override fun merge(v: BasicValue?, w: BasicValue?): BasicValue? =
        if (v is PossibleLambdaLoad && w is PossibleLambdaLoad && v.insn == w.insn) v else super.merge(v, w)
}
