/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.backend.common.isTopLevelInPackage
import org.jetbrains.kotlin.codegen.coroutines.createCustomCopy
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.JVMAssertionsMode
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.MethodNode

val assertionsDisabledFieldName = "\$assertionsDisabled"
private const val ALWAYS_ENABLED_ASSERT_FUNCTION_NAME = "alwaysEnabledAssert"
private const val LAMBDA_INTERNAL_NAME = "kotlin/jvm/functions/Function0"
private const val ASSERTION_ERROR_INTERNAL_NAME = "java/lang/AssertionError"
private const val THROWABLE_INTERNAL_NAME = "java/lang/Throwable"

fun isAssertCall(resolvedCall: ResolvedCall<*>) = resolvedCall.resultingDescriptor.isTopLevelInPackage("assert", "kotlin")

private fun FunctionDescriptor.isBuiltinAlwaysEnabledAssertWithLambda() =
    this.isTopLevelInPackage(ALWAYS_ENABLED_ASSERT_FUNCTION_NAME, "kotlin") && this.valueParameters.size == 2

private fun FunctionDescriptor.isBuiltinAlwaysEnabledAssertWithoutLambda() =
    this.isTopLevelInPackage(ALWAYS_ENABLED_ASSERT_FUNCTION_NAME, "kotlin") && this.valueParameters.size == 1

fun FunctionDescriptor.isBuiltinAlwaysEnabledAssert() =
    this.isBuiltinAlwaysEnabledAssertWithLambda() || this.isBuiltinAlwaysEnabledAssertWithoutLambda()

fun createMethodNodeForAlwaysEnabledAssert(
    functionDescriptor: FunctionDescriptor,
    typeMapper: KotlinTypeMapper
): MethodNode {
    assert(functionDescriptor.isBuiltinAlwaysEnabledAssert()) {
        "functionDescriptor must be kotlin.alwaysEnabledAssert, but got $functionDescriptor"
    }

    val node =
        org.jetbrains.org.objectweb.asm.tree.MethodNode(
            Opcodes.ASM5,
            Opcodes.ACC_STATIC,
            "fake",
            typeMapper.mapAsmMethod(functionDescriptor).descriptor, null, null
        )

    val v = InstructionAdapter(node)
    val returnLabel = Label()

    // if (!condition)
    v.load(0, Type.BOOLEAN_TYPE)
    v.ifne(returnLabel)
    if (functionDescriptor.isBuiltinAlwaysEnabledAssertWithLambda()) {
        // val err = AssertionError(lambda())
        v.load(1, Type.getObjectType(LAMBDA_INTERNAL_NAME))
        v.invokeinterface(LAMBDA_INTERNAL_NAME, "invoke", "()Ljava/lang/Object;")
        v.store(2, AsmTypes.OBJECT_TYPE)
        v.anew(Type.getObjectType(ASSERTION_ERROR_INTERNAL_NAME))
        v.dup()
        v.load(2, AsmTypes.OBJECT_TYPE)
        v.invokespecial(ASSERTION_ERROR_INTERNAL_NAME, "<init>", "(Ljava/lang/Object;)V", false)
    } else {
        // val err = AssertionError("Assertion failed")
        v.anew(Type.getObjectType(ASSERTION_ERROR_INTERNAL_NAME))
        v.dup()
        v.visitLdcInsn("Assertion failed")
        v.invokespecial(ASSERTION_ERROR_INTERNAL_NAME, "<init>", "(Ljava/lang/Object;)V", false)
    }
    // throw err
    v.checkcast(Type.getObjectType(THROWABLE_INTERNAL_NAME))
    v.athrow()
    // else return
    v.mark(returnLabel)
    v.areturn(Type.VOID_TYPE)
    node.visitMaxs(3, 3)

    return node
}

fun generateAssert(
    assertionsMode: JVMAssertionsMode,
    resolvedCall: ResolvedCall<*>,
    codegen: ExpressionCodegen,
    parentCodegen: MemberCodegen<*>
) {
    assert(isAssertCall(resolvedCall)) { "generateAssert expects call of kotlin.assert function" }
    when (assertionsMode) {
        JVMAssertionsMode.ALWAYS_ENABLE -> inlineAlwaysInlineAssert(resolvedCall, codegen)
        JVMAssertionsMode.ALWAYS_DISABLE -> {
            // Nothing to do: assertions disabled
        }
        JVMAssertionsMode.JVM -> generateJvmAssert(resolvedCall, codegen, parentCodegen)
        else -> error("legacy assertions mode shall be handled in ExpressionCodegen")
    }
}

private fun generateJvmAssert(resolvedCall: ResolvedCall<*>, codegen: ExpressionCodegen, parentCodegen: MemberCodegen<*>) {
    parentCodegen.generateAssertField()

    val label = Label()
    with(codegen.v) {
        getstatic(parentCodegen.v.thisName, "\$assertionsDisabled", "Z")
        ifne(label)
        inlineAlwaysInlineAssert(resolvedCall, codegen)
        mark(label)
    }
}

@Suppress("UNCHECKED_CAST")
private fun inlineAlwaysInlineAssert(resolvedCall: ResolvedCall<*>, codegen: ExpressionCodegen) {
    val replaced = (resolvedCall as ResolvedCall<FunctionDescriptor>).replaceAssertWithAssertInner()
    codegen.invokeMethodWithArguments(
        codegen.typeMapper.mapToCallableMethod(replaced.resultingDescriptor, false),
        replaced,
        StackValue.none()
    )
}

fun generateAssertionsDisabledFieldInitialization(parentCodegen: MemberCodegen<*>) {
    parentCodegen.v.newField(
        JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_STATIC or Opcodes.ACC_FINAL or Opcodes.ACC_SYNTHETIC, assertionsDisabledFieldName,
        "Z", null, null
    )
    val clInitCodegen = parentCodegen.createOrGetClInitCodegen()
    MemberCodegen.markLineNumberForElement(parentCodegen.element.psiOrParent, clInitCodegen.v)
    val thenLabel = Label()
    val elseLabel = Label()
    with(clInitCodegen.v) {
        aconst(Type.getObjectType(parentCodegen.v.thisName))
        invokevirtual("java/lang/Class", "desiredAssertionStatus", "()Z", false)
        ifne(thenLabel)
        iconst(1)
        goTo(elseLabel)

        mark(thenLabel)
        iconst(0)

        mark(elseLabel)
        putstatic(parentCodegen.v.thisName, assertionsDisabledFieldName, "Z")
    }
}

private fun <D : FunctionDescriptor> ResolvedCall<D>.replaceAssertWithAssertInner(): ResolvedCall<D> {
    val newCandidateDescriptor = resultingDescriptor.createCustomCopy {
        setName(Name.identifier(ALWAYS_ENABLED_ASSERT_FUNCTION_NAME))
    }
    val newResolvedCall = ResolvedCallImpl(
        call,
        newCandidateDescriptor,
        dispatchReceiver, extensionReceiver, explicitReceiverKind,
        null, DelegatingBindingTrace(BindingTraceContext().bindingContext, "Temporary trace for assertInner"),
        TracingStrategy.EMPTY, MutableDataFlowInfoForArguments.WithoutArgumentsCheck(DataFlowInfo.EMPTY)
    )
    valueArguments.forEach {
        newResolvedCall.recordValueArgument(newCandidateDescriptor.valueParameters[it.key.index], it.value)
    }
    return newResolvedCall
}
