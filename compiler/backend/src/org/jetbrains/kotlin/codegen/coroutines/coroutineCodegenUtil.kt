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
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

// These classes do not actually exist at runtime
val CONTINUATION_METHOD_ANNOTATION_DESC = "Lkotlin/ContinuationMethod;"

const val SUSPENSION_POINT_MARKER_OWNER = "kotlin/Markers"
const val SUSPENSION_POINT_MARKER_NAME = "suspensionPoint"

const val COROUTINE_CONTROLLER_FIELD_NAME = "controller"
const val COROUTINE_LABEL_FIELD_NAME = "label"

data class ResolvedCallWithRealDescriptor(val resolvedCall: ResolvedCall<*>, val fakeThisExpression: KtExpression)

// Resolved calls to suspension function contain descriptors as they visible within coroutines:
// E.g. `fun <V> await(f: CompletableFuture<V>): V` instead of `fun <V> await(f: CompletableFuture<V>, machine: Continuation<V>): Unit`
// See `createCoroutineSuspensionFunctionView` and it's usages for clarification
// But for call generation it's convenient to have `machine` (continuation) parameter/argument within resolvedCall.
// So this function returns resolved call with descriptor looking like `fun <V> await(f: CompletableFuture<V>, machine: Continuation<V>): V`
// and fake `this` expression that used as argument for second parameter
fun ResolvedCall<*>.replaceSuspensionFunctionViewWithRealDescriptor(
        project: Project
): ResolvedCallWithRealDescriptor? {
    val function = candidateDescriptor as? FunctionDescriptor ?: return null
    if (!function.isSuspend) return null

    val initialSignatureDescriptor = function.initialSignatureDescriptor ?: return null
    val newCandidateDescriptor =
            initialSignatureDescriptor.createCustomCopy {
                // Here we know that last parameter should be Continuation<T> where T is return type
                setReturnType(it.valueParameters.last().type.arguments.single().type)
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

    newCall.setResultingSubstitutor(
            TypeConstructorSubstitution.createByParametersMap(typeArguments.mapValues { it.value.asTypeProjection() }).buildSubstitutor())

    return ResolvedCallWithRealDescriptor(newCall, thisExpression)
}

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
