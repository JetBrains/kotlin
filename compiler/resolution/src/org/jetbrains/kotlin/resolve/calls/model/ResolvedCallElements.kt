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

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.components.ArgumentsToParametersMapper
import org.jetbrains.kotlin.resolve.calls.inference.model.LambdaTypeVariable
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.builtIns


sealed class ArgumentWithPostponeResolution {
    abstract val outerCall: KotlinCall
    abstract val argument: KotlinCallArgument
    abstract val myTypeVariables: Collection<NewTypeVariable>
    abstract val inputType: Collection<UnwrappedType> // parameters and implicit receiver
    abstract val outputType: UnwrappedType?

    var analyzed: Boolean = false
}

class ResolvedLambdaArgument(
        override val outerCall: KotlinCall,
        override val argument: LambdaKotlinCallArgument,
        override val myTypeVariables: Collection<LambdaTypeVariable>,
        val receiver: UnwrappedType?,
        val parameters: List<UnwrappedType>,
        val returnType: UnwrappedType
) : ArgumentWithPostponeResolution() {
    val type: SimpleType = createFunctionType(returnType.builtIns, Annotations.EMPTY, receiver, parameters, null, returnType) // todo support annotations

    override val inputType: Collection<UnwrappedType> get() = receiver?.let { parameters + it } ?: parameters
    override val outputType: UnwrappedType get() = returnType

    lateinit var resultArguments: List<KotlinCallArgument>
}


class ResolvedPropertyReference(
        val outerCall: KotlinCall,
        val argument: ChosenCallableReferenceDescriptor,
        val reflectionType: UnwrappedType
) {
    val boundDispatchReceiver: ReceiverValue? get() = argument.candidate.dispatchReceiver?.receiverValue?.takeIf { it !is MockReceiverForCallableReference }
    val boundExtensionReceiver: ReceiverValue? get() = argument.extensionReceiver?.receiverValue?.takeIf { it !is MockReceiverForCallableReference }
}

class ResolvedFunctionReference(
        val outerCall: KotlinCall,
        val argument: ChosenCallableReferenceDescriptor,
        val reflectionType: UnwrappedType,
        val argumentsMapping: ArgumentsToParametersMapper.ArgumentMapping?
) {
    val boundDispatchReceiver: ReceiverValue? get() = argument.candidate.dispatchReceiver?.receiverValue?.takeIf { it !is MockReceiverForCallableReference }
    val boundExtensionReceiver: ReceiverValue? get() = argument.extensionReceiver?.receiverValue?.takeIf { it !is MockReceiverForCallableReference }
}


fun KotlinCall.getExplicitDispatchReceiver(explicitReceiverKind: ExplicitReceiverKind) = when (explicitReceiverKind) {
    ExplicitReceiverKind.DISPATCH_RECEIVER -> explicitReceiver
    ExplicitReceiverKind.BOTH_RECEIVERS -> dispatchReceiverForInvokeExtension
    else -> null
}

fun KotlinCall.getExplicitExtensionReceiver(explicitReceiverKind: ExplicitReceiverKind) = when (explicitReceiverKind) {
    ExplicitReceiverKind.EXTENSION_RECEIVER, ExplicitReceiverKind.BOTH_RECEIVERS -> explicitReceiver
    else -> null
}

class MockReceiverForCallableReference(val lhsOrDeclaredType: UnwrappedType) : ReceiverValue {
    override fun getType() = lhsOrDeclaredType
    override fun replaceType(newType: KotlinType) = MockReceiverForCallableReference(newType.unwrap())
}

val ChosenCallableReferenceDescriptor.dispatchNotBoundReceiver : UnwrappedType?
    get() = (candidate.dispatchReceiver?.receiverValue as? MockReceiverForCallableReference)?.lhsOrDeclaredType

val ChosenCallableReferenceDescriptor.extensionNotBoundReceiver : UnwrappedType?
    get() = (extensionReceiver as? MockReceiverForCallableReference)?.lhsOrDeclaredType
