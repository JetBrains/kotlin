/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline.coroutines

import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.codegen.AsmUtil.CAPTURED_THIS_FIELD
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.TransformationMethodVisitor
import org.jetbrains.kotlin.codegen.coroutines.*
import org.jetbrains.kotlin.codegen.coroutines.getLastParameterIndex
import org.jetbrains.kotlin.codegen.coroutines.replaceFakeContinuationsWithRealOnes
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.common.findPreviousOrNull
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceInterpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceValue

const val NOINLINE_CALL_MARKER = "NOINLINE_CALL_MARKER"

const val FOR_INLINE_SUFFIX = "\$\$forInline"

class CoroutineTransformer(
    private val inliningContext: InliningContext,
    private val classBuilder: ClassBuilder,
    private val methods: List<MethodNode>,
    private val superClassName: String,
    private val capturedParams: List<CapturedParamInfo>
) {
    private val state = inliningContext.state
    // If we inline into inline function, we should generate both method with state-machine for Java interop and method without
    // state-machine for further transformation/inlining.
    private val generateForInline = inliningContext.callSiteInfo.isInlineOrInsideInline

    fun shouldSkip(node: MethodNode): Boolean = methods.any { it.name == node.name + FOR_INLINE_SUFFIX && it.desc == node.desc }

    fun shouldGenerateStateMachine(node: MethodNode): Boolean {
        // Continuations are similar to lambdas from bird's view, but we should never generate state machine for them
        if (isContinuationNotLambda()) return false
        // The method does not have state-machine, but should. Generate it
        if (node.name.endsWith(FOR_INLINE_SUFFIX)) return true
        // there can be suspend lambdas inside inline functions, which do not
        // capture crossinline lambdas, thus, there is no need to transform them
        return isSuspendFunctionWithFakeConstructorCall(node) || (isSuspendLambda(node) && !isStateMachine(node))
    }

    private fun isContinuationNotLambda(): Boolean = inliningContext.isContinuation &&
            if (state.languageVersionSettings.isReleaseCoroutines()) superClassName.endsWith("ContinuationImpl")
            else methods.any { it.name == "getLabel" }

    private fun crossinlineLambda(): PsiExpressionLambda? = inliningContext.expressionMap.values.find {
        it is PsiExpressionLambda && it.isCrossInline
    }?.cast()

    private fun isStateMachine(node: MethodNode): Boolean =
        node.instructions.asSequence().any { insn -> insn is LdcInsnNode && insn.cst == ILLEGAL_STATE_ERROR_MESSAGE }

    private fun isSuspendLambda(node: MethodNode) = isResumeImpl(node)

    fun newMethod(node: MethodNode): DeferredMethodVisitor {
        // Find ANY element to report error about suspension point in monitor on.
        val element = crossinlineLambda()?.functionWithBodyOrCallableReference
            ?: inliningContext.root.sourceCompilerForInline.callElement as? KtElement
            ?: error("crossinline lambda should have element")

        return when {
            isResumeImpl(node) -> {
                assert(!isStateMachine(node)) {
                    "Inlining/transforming state-machine"
                }
                newStateMachineForLambda(node, element)
            }
            isSuspendFunctionWithFakeConstructorCall(node) -> newStateMachineForNamedFunction(node, element)
            else -> error("no need to generate state maching for ${node.name}")
        }
    }

    private fun isResumeImpl(node: MethodNode): Boolean =
        state.languageVersionSettings.isResumeImplMethodName(node.name.removeSuffix(FOR_INLINE_SUFFIX)) &&
                inliningContext.isContinuation

    private fun isSuspendFunctionWithFakeConstructorCall(node: MethodNode): Boolean = findFakeContinuationConstructorClassName(node) != null

    private fun newStateMachineForLambda(node: MethodNode, element: KtElement): DeferredMethodVisitor {
        val name = node.name.removeSuffix(FOR_INLINE_SUFFIX)
        return DeferredMethodVisitor(
            MethodNode(
                node.access, name, node.desc, node.signature,
                ArrayUtil.toStringArray(node.exceptions)
            )
        ) {
            val stateMachineBuilder = surroundNoinlineCallsWithMarkersIfNeeded(
                node,
                CoroutineTransformerMethodVisitor(
                    createNewMethodFrom(node, name), node.access, name, node.desc, null, null,
                    obtainClassBuilderForCoroutineState = { classBuilder },
                    element = element,
                    diagnostics = state.diagnostics,
                    languageVersionSettings = state.languageVersionSettings,
                    shouldPreserveClassInitialization = state.constructorCallNormalizationMode.shouldPreserveClassInitialization,
                    containingClassInternalName = classBuilder.thisName,
                    isForNamedFunction = false
                )
            )

            if (generateForInline)
                MethodNodeCopyingMethodVisitor(
                    delegate = stateMachineBuilder,
                    access = node.access,
                    name = name,
                    desc = node.desc,
                    newMethod = { origin, newAccess, newName, newDesc ->
                        classBuilder.newMethod(origin, newAccess, newName, newDesc, null, null)
                    }
                )
            else
                stateMachineBuilder
        }
    }

    private fun newStateMachineForNamedFunction(node: MethodNode, element: KtElement): DeferredMethodVisitor {
        val name = node.name.removeSuffix(FOR_INLINE_SUFFIX)
        val continuationClassName = findFakeContinuationConstructorClassName(node)
        assert(inliningContext is RegeneratedClassContext)
        return DeferredMethodVisitor(
            MethodNode(
                node.access, name, node.desc, node.signature,
                ArrayUtil.toStringArray(node.exceptions)
            )
        ) {
            val stateMachineBuilder = surroundNoinlineCallsWithMarkersIfNeeded(
                node,
                CoroutineTransformerMethodVisitor(
                    createNewMethodFrom(node, name), node.access, name, node.desc, null, null,
                    obtainClassBuilderForCoroutineState = { (inliningContext as RegeneratedClassContext).continuationBuilders[continuationClassName]!! },
                    element = element,
                    diagnostics = state.diagnostics,
                    languageVersionSettings = state.languageVersionSettings,
                    shouldPreserveClassInitialization = state.constructorCallNormalizationMode.shouldPreserveClassInitialization,
                    containingClassInternalName = classBuilder.thisName,
                    isForNamedFunction = true,
                    needDispatchReceiver = true,
                    internalNameForDispatchReceiver = classBuilder.thisName
                )
            )

            if (generateForInline)
                MethodNodeCopyingMethodVisitor(
                    stateMachineBuilder, node.access, name, node.desc,
                    newMethod = { origin, newAccess, newName, newDesc ->
                        classBuilder.newMethod(origin, newAccess, newName, newDesc, null, null)
                    }
                )
            else
                stateMachineBuilder
        }
    }

    private fun surroundNoinlineCallsWithMarkersIfNeeded(node: MethodNode, delegate: MethodVisitor): MethodVisitor =
        if (capturedParams.any { it.functionalArgument?.isSuspendLambda() == true })
            SurroundSuspendLambdaCallsWithSuspendMarkersMethodVisitor(
                delegate, node.access, node.name, node.desc, classBuilder.thisName, this::isCapturedSuspendLambda
            )
        else
            delegate

    private fun isCapturedSuspendLambda(field: FieldInsnNode): Boolean =
        capturedParams.find { it.newFieldName == field.name }?.functionalArgument?.isSuspendLambda() == true

    private fun createNewMethodFrom(node: MethodNode, name: String): MethodVisitor {
        return classBuilder.newMethod(
            JvmDeclarationOrigin.NO_ORIGIN, node.access, name, node.desc, node.signature, ArrayUtil.toStringArray(node.exceptions)
        )
    }

    fun replaceFakesWithReals(node: MethodNode) {
        findFakeContinuationConstructorClassName(node)?.let(::unregisterClassBuilder)?.let(ClassBuilder::done)
        replaceFakeContinuationsWithRealOnes(
            node, if (!inliningContext.isContinuation) getLastParameterIndex(node.desc, node.access) else 0
        )
    }

    fun registerClassBuilder(continuationClassName: String) {
        val context = inliningContext.parent?.parent as? RegeneratedClassContext ?: error("incorrect context")
        context.continuationBuilders[continuationClassName] = classBuilder
    }

    fun unregisterClassBuilder(continuationClassName: String) =
        (inliningContext as RegeneratedClassContext).continuationBuilders.remove(continuationClassName)

    companion object {
        fun findFakeContinuationConstructorClassName(node: MethodNode): String? {
            val marker = node.instructions.asSequence().firstOrNull(::isBeforeFakeContinuationConstructorCallMarker) ?: return null
            val new = marker.next
            assert(new?.opcode == Opcodes.NEW)
            return (new as TypeInsnNode).desc
        }
    }
}

class SurroundSuspendLambdaCallsWithSuspendMarkersMethodVisitor(
    delegate: MethodVisitor, access: Int, name: String, desc: String,
    private val thisName: String,
    private val isCapturedSuspendLambda: (FieldInsnNode) -> Boolean
) : TransformationMethodVisitor(delegate, access, name, desc, null, null) {
    override fun performTransformations(methodNode: MethodNode) {
        fun AbstractInsnNode.index() = methodNode.instructions.indexOf(this)

        val sourceFrames = MethodTransformer.analyze(thisName, methodNode, SourceInterpreter())

        val noinlineInvokes = arrayListOf<Pair<AbstractInsnNode, AbstractInsnNode>>()

        for (insn in methodNode.instructions.asSequence()) {
            if (insn.opcode != Opcodes.INVOKEINTERFACE) continue
            insn as MethodInsnNode
            if (!isInvokeOnLambda(insn.owner, insn.name)) continue
            val frame = sourceFrames[insn.index()]
            val receiver = findReceiverOfInvoke(frame, insn).takeIf { it?.isSuspendLambda(insn) == true } as? FieldInsnNode ?: continue
            val aload = receiver.findPreviousOrNull { it.opcode != Opcodes.GETFIELD } ?: error("GETFIELD cannot be the first instruction")
            assert(aload.opcode == Opcodes.ALOAD) { "Before GETFIELD there shall be ALOAD" }
            noinlineInvokes.add(insn to aload)
        }

        surroundInvokesWithSuspendMarkers(methodNode, noinlineInvokes)
    }

    private fun AbstractInsnNode.isSuspendLambda(invoke: MethodInsnNode): Boolean {
        if (opcode != Opcodes.GETFIELD) return false
        this as FieldInsnNode
        if (desc != "L${invoke.owner};") return false
        var current: FieldInsnNode? = this
        // Unroll the battery of
        // GETFIELD <outer1>.this$0 L<outer2>;
        // GETFIELD <outer2>.this$0 L<outer3>;
        // ...
        // GETFIELD <outerN>.$action Lkotlin/jvm/functions/FunctionM;
        while (current != null) {
            if (current.owner == thisName) break
            if (current.previous?.opcode != Opcodes.GETFIELD || current.previous.cast<FieldInsnNode>().name != CAPTURED_THIS_FIELD) return false
            current = current.previous as FieldInsnNode
        }
        return isCapturedSuspendLambda(this)
    }
}

private fun FunctionalArgument.isSuspendLambda(): Boolean =
    (this is NonInlineableArgumentForInlineableParameterCalledInSuspend && isSuspend) ||
            (this is PsiExpressionLambda && invokeMethodDescriptor.isSuspend)

fun surroundInvokesWithSuspendMarkers(
    methodNode: MethodNode,
    noinlineInvokes: List<Pair<AbstractInsnNode, AbstractInsnNode>>
) {
    for ((invoke, aload) in noinlineInvokes) {
        // Generate inline markers for stack transformation. It is required for local variables spilling.
        methodNode.instructions.insertBefore(aload, withInstructionAdapter {
            addInlineMarker(this, isStartNotEnd = true)
        })
        methodNode.instructions.insertBefore(invoke, withInstructionAdapter {
            addSuspendMarker(this, isStartNotEnd = true)
        })
        methodNode.instructions.insert(invoke, withInstructionAdapter {
            addSuspendMarker(this, isStartNotEnd = false)
            addInlineMarker(this, isStartNotEnd = false)
        })
    }
}

// TODO: What to do if suddenly there are not exactly one receiver?
fun findReceiverOfInvoke(frame: Frame<SourceValue>, insn: MethodInsnNode): AbstractInsnNode? =
    frame.getStack(frame.stackSize - insn.owner.removePrefix(NUMBERED_FUNCTION_PREFIX).toInt() - 1)?.insns?.singleOrNull()

fun AbstractInsnNode.isNoinlineCallMarker(): Boolean =
    opcode == Opcodes.INVOKESTATIC && cast<MethodInsnNode>().let { it.owner == NOINLINE_CALL_MARKER && it.name == NOINLINE_CALL_MARKER }
