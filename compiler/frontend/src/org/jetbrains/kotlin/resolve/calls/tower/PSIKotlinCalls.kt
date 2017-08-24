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

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategyForInvoke
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.util.OperatorNameConventions

val KotlinCall.psiKotlinCall: PSIKotlinCall get() {
    assert(this is PSIKotlinCall) {
        "Incorrect ASTCAll: $this. Java class: ${javaClass.canonicalName}"
    }
    return this as PSIKotlinCall
}

abstract class PSIKotlinCall : KotlinCall {
    abstract val psiCall: Call
    abstract val startingDataFlowInfo: DataFlowInfo
    abstract val resultDataFlowInfo: DataFlowInfo
    abstract val dataFlowInfoForArguments: DataFlowInfoForArguments
    abstract val tracingStrategy: TracingStrategy

    override fun toString() = "$psiCall"
}

class PSIKotlinCallImpl(
        override val callKind: KotlinCallKind,
        override val psiCall: Call,
        override val tracingStrategy: TracingStrategy,
        override val explicitReceiver: ReceiverKotlinCallArgument?,
        override val name: Name,
        override val typeArguments: List<TypeArgument>,
        override val argumentsInParenthesis: List<KotlinCallArgument>,
        override val externalArgument: KotlinCallArgument?,
        override val startingDataFlowInfo: DataFlowInfo,
        override val resultDataFlowInfo: DataFlowInfo,
        override val dataFlowInfoForArguments: DataFlowInfoForArguments
) : PSIKotlinCall()

class PSIKotlinCallForVariable(
        val baseCall: PSIKotlinCallImpl,
        override val explicitReceiver: ReceiverKotlinCallArgument?,
        override val name: Name
) : PSIKotlinCall() {
    override val callKind: KotlinCallKind get() = KotlinCallKind.VARIABLE
    override val typeArguments: List<TypeArgument> get() = emptyList()
    override val argumentsInParenthesis: List<KotlinCallArgument> get() = emptyList()
    override val externalArgument: KotlinCallArgument? get() = null

    override val startingDataFlowInfo: DataFlowInfo get() = baseCall.startingDataFlowInfo
    override val resultDataFlowInfo: DataFlowInfo get() = baseCall.startingDataFlowInfo
    override val dataFlowInfoForArguments: DataFlowInfoForArguments get() = baseCall.dataFlowInfoForArguments

    override val tracingStrategy: TracingStrategy get() = baseCall.tracingStrategy
    override val psiCall: Call = CallTransformer.stripCallArguments(baseCall.psiCall).let {
        if (explicitReceiver == null) CallTransformer.stripReceiver(it) else it
    }
}

class PSIKotlinCallForInvoke(
        val baseCall: PSIKotlinCallImpl,
        val variableCall: KotlinResolutionCandidate,
        override val explicitReceiver: ReceiverKotlinCallArgument,
        override val dispatchReceiverForInvokeExtension: SimpleKotlinCallArgument?
) : PSIKotlinCall() {
    override val callKind: KotlinCallKind get() = KotlinCallKind.FUNCTION
    override val name: Name get() = OperatorNameConventions.INVOKE
    override val typeArguments: List<TypeArgument> get() = baseCall.typeArguments
    override val argumentsInParenthesis: List<KotlinCallArgument> get() = baseCall.argumentsInParenthesis
    override val externalArgument: KotlinCallArgument? get() = baseCall.externalArgument

    override val startingDataFlowInfo: DataFlowInfo get() = baseCall.startingDataFlowInfo
    override val resultDataFlowInfo: DataFlowInfo get() = baseCall.resultDataFlowInfo
    override val dataFlowInfoForArguments: DataFlowInfoForArguments get() = baseCall.dataFlowInfoForArguments
    override val psiCall: Call
    override val tracingStrategy: TracingStrategy

    init {
        val variableReceiver = dispatchReceiverForInvokeExtension ?: explicitReceiver
        val explicitExtensionReceiver = if (dispatchReceiverForInvokeExtension == null) null else explicitReceiver
        val calleeExpression = baseCall.psiCall.calleeExpression!!

        psiCall = CallTransformer.CallForImplicitInvoke(
                explicitExtensionReceiver?.receiverValue,
                variableReceiver.receiverValue as ExpressionReceiver, baseCall.psiCall, true)
        tracingStrategy = TracingStrategyForInvoke(calleeExpression, psiCall, variableReceiver.receiverValue!!.type) // check for type parameters
    }
}

val ReceiverKotlinCallArgument.receiverValue: ReceiverValue?
    get() = when (this) {
        is SimpleKotlinCallArgument -> this.receiver.receiverValue
        is QualifierReceiverKotlinCallArgument -> this.receiver.classValueReceiver
        else -> null
    }