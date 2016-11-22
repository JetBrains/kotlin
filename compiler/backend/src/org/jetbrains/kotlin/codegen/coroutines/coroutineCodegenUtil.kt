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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.coroutine.REPLACED_SUSPENSION_POINT_KEY
import org.jetbrains.kotlin.resolve.coroutine.SUSPENSION_POINT_KEY
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.util.OperatorNameConventions

// These classes do not actually exist at runtime
val CONTINUATION_METHOD_ANNOTATION_DESC = "Lkotlin/ContinuationMethod;"

const val COROUTINE_MARKER_OWNER = "kotlin/coroutines/Markers"
const val BEFORE_SUSPENSION_POINT_MARKER_NAME = "beforeSuspensionPoint"
const val AFTER_SUSPENSION_POINT_MARKER_NAME = "afterSuspensionPoint"
const val HANDLE_EXCEPTION_MARKER_NAME = "handleException"
const val HANDLE_EXCEPTION_ARGUMENT_MARKER_NAME = "handleExceptionArgument"
const val ACTUAL_COROUTINE_START_MARKER_NAME = "actualCoroutineStart"

const val COROUTINE_CONTROLLER_FIELD_NAME = "_controller"
const val COROUTINE_CONTROLLER_GETTER_NAME = "getController"
const val COROUTINE_LABEL_FIELD_NAME = "label"

data class ResolvedCallWithRealDescriptor(val resolvedCall: ResolvedCall<*>, val fakeThisExpression: KtExpression)

// Resolved calls to suspension function contain descriptors as they visible within coroutines:
// E.g. `fun <V> await(f: CompletableFuture<V>): V` instead of `fun <V> await(f: CompletableFuture<V>, machine: Continuation<V>): Unit`
// See `createCoroutineSuspensionFunctionView` and it's usages for clarification
// But for call generation it's convenient to have `machine` (continuation) parameter/argument within resolvedCall.
// So this function returns resolved call with descriptor looking like `fun <V> await(f: CompletableFuture<V>, machine: Continuation<V>): Unit`
// and fake `this` expression that used as argument for second parameter
fun ResolvedCall<*>.replaceSuspensionFunctionViewWithRealDescriptor(
        project: Project
): ResolvedCallWithRealDescriptor? {
    val function = candidateDescriptor as? FunctionDescriptor ?: return null
    if (!isSuspensionPoint()) return null

    val initialSignatureDescriptor = function.initialSignatureDescriptor ?: return null
    if (function.getUserData(REPLACED_SUSPENSION_POINT_KEY) == true) return null

    val newCandidateDescriptor =
            initialSignatureDescriptor.createCustomCopy {
                setPreserveSourceElement()
                setSignatureChange()
                putUserData(SUSPENSION_POINT_KEY, true)
                putUserData(REPLACED_SUSPENSION_POINT_KEY, true)
            }

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

fun ResolvedCall<*>.isSuspensionPoint() =
        (candidateDescriptor as? FunctionDescriptor)?.let { it.isSuspend && it.getUserData(SUSPENSION_POINT_KEY) ?: false }
        ?: false

private fun FunctionDescriptor.createCustomCopy(
        copySettings: FunctionDescriptor.CopyBuilder<out FunctionDescriptor>.(FunctionDescriptor) -> FunctionDescriptor.CopyBuilder<out FunctionDescriptor>
): FunctionDescriptor {

    val newOriginal =
            if (original !== this)
                original.createCustomCopy(copySettings)
            else
                null

    val result = newCopyBuilder().copySettings(this).setOriginal(newOriginal).build()!!

    result.overriddenDescriptors = this.overriddenDescriptors.map { it.createCustomCopy(copySettings) }

    return result
}

fun KotlinType.hasInlineInterceptResume() =
        findOperatorInController(this, OperatorNameConventions.COROUTINE_INTERCEPT_RESUME)?.isInline == true

fun KotlinType.hasNoinlineInterceptResume() =
        findOperatorInController(this, OperatorNameConventions.COROUTINE_INTERCEPT_RESUME)?.isInline == false

fun findOperatorInController(controllerType: KotlinType, name: Name): SimpleFunctionDescriptor? =
        controllerType.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND).singleOrNull { it.isOperator }
