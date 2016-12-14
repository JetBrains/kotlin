/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.coroutines

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.SUSPEND_WITH_CURRENT_CONTINUATION_NAME
import org.jetbrains.kotlin.backend.common.getBuiltInSuspendWithCurrentContinuation
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.DescriptorEquivalenceForOverrides
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.MethodNode

// These classes do not actually exist at runtime
val CONTINUATION_METHOD_ANNOTATION_DESC = "Lkotlin/ContinuationMethod;"

const val COROUTINE_MARKER_OWNER = "kotlin/coroutines/Markers"
const val BEFORE_SUSPENSION_POINT_MARKER_NAME = "beforeSuspensionPoint"
const val AFTER_SUSPENSION_POINT_MARKER_NAME = "afterSuspensionPoint"
const val HANDLE_EXCEPTION_MARKER_NAME = "handleException"
const val HANDLE_EXCEPTION_ARGUMENT_MARKER_NAME = "handleExceptionArgument"
const val ACTUAL_COROUTINE_START_MARKER_NAME = "actualCoroutineStart"

const val COROUTINE_LABEL_FIELD_NAME = "label"

data class ResolvedCallWithRealDescriptor(val resolvedCall: ResolvedCall<*>, val fakeContinuationExpression: KtExpression)

@JvmField
val INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION = object : FunctionDescriptor.UserDataKey<FunctionDescriptor> {}
// Resolved calls to suspension function contain descriptors as they visible within coroutines:
// E.g. `fun <V> await(f: CompletableFuture<V>): V` instead of `fun <V> await(f: CompletableFuture<V>, machine: Continuation<V>): Unit`
// See `createJvmSuspendFunctionView` and it's usages for clarification
// But for call generation it's convenient to have `machine` (continuation) parameter/argument within resolvedCall.
// So this function returns resolved call with descriptor looking like `fun <V> await(f: CompletableFuture<V>, machine: Continuation<V>): Unit`
// and fake `this` expression that used as argument for second parameter
fun ResolvedCall<*>.replaceSuspensionFunctionWithRealDescriptor(
        project: Project,
        bindingContext: BindingContext
): ResolvedCallWithRealDescriptor? {
    val function = candidateDescriptor as? SimpleFunctionDescriptor ?: return null
    if (!function.isSuspend || function.getUserData(INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION) != null) return null

    val newCandidateDescriptor =
            bindingContext.get(CodegenBinding.SUSPEND_FUNCTION_TO_JVM_VIEW, function)
            ?: createJvmSuspendFunctionView(function)

    val newCall = ResolvedCallImpl(
            call,
            newCandidateDescriptor,
            dispatchReceiver, extensionReceiver, explicitReceiverKind,
            null, DelegatingBindingTrace(BindingTraceContext().bindingContext, "Temporary trace for unwrapped suspension function"),
            TracingStrategy.EMPTY, MutableDataFlowInfoForArguments.WithoutArgumentsCheck(DataFlowInfo.EMPTY))

    this.valueArguments.forEach {
        newCall.recordValueArgument(newCandidateDescriptor.valueParameters[it.key.index], it.value)
    }

    val psiFactory = KtPsiFactory(project)
    val arguments = psiFactory.createCallArguments("(this)").arguments.single()
    val thisExpression = arguments.getArgumentExpression()!!
    newCall.recordValueArgument(
            newCandidateDescriptor.valueParameters.last(),
            ExpressionValueArgument(arguments))

    val newTypeArguments = newCandidateDescriptor.typeParameters.map {
        Pair(it, typeArguments[candidateDescriptor.typeParameters[it.index]]!!.asTypeProjection())
    }.toMap()

    newCall.setResultingSubstitutor(
            TypeConstructorSubstitution.createByParametersMap(newTypeArguments).buildSubstitutor())

    return ResolvedCallWithRealDescriptor(newCall, thisExpression)
}

data class HandleResultCallContext(
        val resolvedCall: ResolvedCall<*>,
        val exceptionExpression: KtExpression,
        val continuationThisExpression: KtExpression
)

fun createResolvedCallForHandleExceptionCall(
        callElement: KtElement,
        handleExceptionFunction: SimpleFunctionDescriptor,
        coroutineLambdaDescriptor: FunctionDescriptor
): HandleResultCallContext {
    val psiFactory = KtPsiFactory(callElement)

    val exceptionArgument = CallMaker.makeValueArgument(psiFactory.createExpression("exception"))
    val continuationThisArgument = CallMaker.makeValueArgument(psiFactory.createExpression("this"))

    val resolvedCall =
            createFakeResolvedCall(
                    callElement,
                    coroutineLambdaDescriptor,
                    handleExceptionFunction,
                    listOf(exceptionArgument, continuationThisArgument)
            )

    return HandleResultCallContext(
            resolvedCall, exceptionArgument.getArgumentExpression()!!, continuationThisArgument.getArgumentExpression()!!)
}

private fun createFakeResolvedCall(
        element: KtElement,
        coroutineLambdaDescriptor: FunctionDescriptor,
        descriptor: SimpleFunctionDescriptor,
        valueArguments: List<ValueArgument>
): ResolvedCallImpl<SimpleFunctionDescriptor> {
    val call = CallMaker.makeCall(element, null, null, null, valueArguments)

    val resolvedCall = ResolvedCallImpl(
            call,
            descriptor,
            coroutineLambdaDescriptor.extensionReceiverParameter!!.value, null, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            null, DelegatingBindingTrace(BindingTraceContext().bindingContext, "Temporary trace for handleException resolution"),
            TracingStrategy.EMPTY, MutableDataFlowInfoForArguments.WithoutArgumentsCheck(DataFlowInfo.EMPTY))

    descriptor.valueParameters.zip(valueArguments).forEach {
        resolvedCall.recordValueArgument(it.first, ExpressionValueArgument(it.second))
    }

    resolvedCall.setResultingSubstitutor(TypeSubstitutor.EMPTY)

    return resolvedCall
}

data class InterceptResumeCallContext(
        val resolvedCall: ResolvedCall<*>,
        val thisExpression: KtExpression
)

fun createResolvedCallForInterceptResume(
        coroutineLambda: KtFunctionLiteral,
        interceptResume: SimpleFunctionDescriptor,
        coroutineLambdaDescriptor: FunctionDescriptor
): InterceptResumeCallContext {
    val psiFactory = KtPsiFactory(coroutineLambda)

    val isInline = interceptResume.isInline
    val thisExpression = psiFactory.createExpression("this")
    val blockArgument = CallMaker.makeValueArgument(if (isInline) coroutineLambda.parent as KtExpression else thisExpression)

    val resolvedCall = createFakeResolvedCall(coroutineLambda, coroutineLambdaDescriptor, interceptResume, listOf(blockArgument))

    return InterceptResumeCallContext(resolvedCall, thisExpression)
}

fun ResolvedCall<*>.isSuspensionPoint(bindingContext: BindingContext) =
        bindingContext[BindingContext.ENCLOSING_SUSPEND_LAMBDA_FOR_SUSPENSION_POINT, call] != null

// Suspend functions have irregular signatures on JVM, containing an additional last parameter with type `Continuation<return-type>`,
// and return type Any?
// This function returns a function descriptor reflecting how the suspend function looks from point of view of JVM
fun <D : FunctionDescriptor> createJvmSuspendFunctionView(function: D): D {
    val continuationParameter = ValueParameterDescriptorImpl(
            function, null, function.valueParameters.size, Annotations.EMPTY, Name.identifier("\$continuation"),
            function.getContinuationParameterTypeOfSuspendFunction(),
            /* declaresDefaultValue = */ false, /* isCrossinline = */ false,
            /* isNoinline = */ false, /* isCoroutine = */ false, /* varargElementType = */ null, SourceElement.NO_SOURCE
    )

    return function.createCustomCopy {
        setPreserveSourceElement()
        setReturnType(function.builtIns.nullableAnyType)
        setValueParameters(it.valueParameters + continuationParameter)
        putUserData(INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION, it)
    }
}

typealias FunctionDescriptorCopyBuilderToFunctionDescriptorCopyBuilder =
    FunctionDescriptor.CopyBuilder<out FunctionDescriptor>.(FunctionDescriptor)
        -> FunctionDescriptor.CopyBuilder<out FunctionDescriptor>

private fun <D : FunctionDescriptor> D.createCustomCopy(
        copySettings: FunctionDescriptorCopyBuilderToFunctionDescriptorCopyBuilder
): D {

    val newOriginal =
            if (original !== this)
                original.createCustomCopy(copySettings)
            else
                null

    val result = newCopyBuilder().copySettings(this).setOriginal(newOriginal).build()!!

    result.overriddenDescriptors = this.overriddenDescriptors.map { it.createCustomCopy(copySettings) }

    @Suppress("UNCHECKED_CAST")
    return result as D
}

private fun FunctionDescriptor.getContinuationParameterTypeOfSuspendFunction() =
        KotlinTypeFactory.simpleType(
                builtIns.continuationClassDescriptor.defaultType,
                arguments = listOf(returnType!!.asTypeProjection())
        )

fun FunctionDescriptor.isBuiltInSuspendWithCurrentContinuation(): Boolean {
    if (name != SUSPEND_WITH_CURRENT_CONTINUATION_NAME) return false

    val originalDeclaration = getBuiltInSuspendWithCurrentContinuation() ?: return false

    return DescriptorEquivalenceForOverrides.areEquivalent(
            originalDeclaration, this.getUserData(INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION)
    )
}

fun createMethodNodeForSuspendWithCurrentContinuation(
        functionDescriptor: FunctionDescriptor,
        typeMapper: KotlinTypeMapper
): MethodNode {
    assert(functionDescriptor.isBuiltInSuspendWithCurrentContinuation()) {
        "functionDescriptor must be kotlin.coroutines.suspendWithCurrentContinuation"
    }

    val node =
            MethodNode(
                    Opcodes.ASM5,
                    Opcodes.ACC_STATIC,
                    "fake",
                    typeMapper.mapAsmMethod(functionDescriptor).descriptor, null, null
            )

    node.visitVarInsn(Opcodes.ALOAD, 0)
    node.visitVarInsn(Opcodes.ALOAD, 1)
    node.visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            typeMapper.mapType(functionDescriptor.valueParameters[0]).internalName,
            OperatorNameConventions.INVOKE.identifier,
            "(${AsmTypes.OBJECT_TYPE})${AsmTypes.OBJECT_TYPE}",
            true
    )
    node.visitInsn(Opcodes.ARETURN)
    node.visitMaxs(2, 2)

    return node
}

fun CallableDescriptor?.unwrapInitialDescriptorForSuspendFunction() =
        (this as? SimpleFunctionDescriptor)?.getUserData(INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION) ?: this

fun InstructionAdapter.loadSuspendMarker() = invokestatic(
        AsmTypes.COROUTINES_SUSPEND_MARKER_OWNER.internalName,
        "getSUSPENDED",
        Type.getMethodDescriptor(AsmTypes.OBJECT_TYPE),
        false
)
