/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.tasks

import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor.Kind.Function
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.ArrayList

fun createSynthesizedInvokes(functions: Collection<FunctionDescriptor>): Collection<FunctionDescriptor> {
    val result = ArrayList<FunctionDescriptor>(1)

    for (invoke in functions) {
        if (invoke !is FunctionInvokeDescriptor || invoke.getValueParameters().isEmpty()) continue

        val synthesized = if ((invoke.getContainingDeclaration() as? FunctionClassDescriptor)?.functionKind == Function) {
            createSynthesizedFunctionWithFirstParameterAsReceiver(invoke)
        }
        else {
            val invokeDeclaration = invoke.getOverriddenDescriptors().single()
            val synthesizedSuperFun = createSynthesizedFunctionWithFirstParameterAsReceiver(invokeDeclaration)
            val fakeOverride = synthesizedSuperFun.copy(
                    invoke.getContainingDeclaration(),
                    synthesizedSuperFun.getModality(),
                    synthesizedSuperFun.getVisibility(),
                    CallableMemberDescriptor.Kind.FAKE_OVERRIDE,
                    true
            )
            fakeOverride.addOverriddenDescriptor(synthesizedSuperFun)
            fakeOverride
        }

        result.add(synthesized.substitute(TypeSubstitutor.create(invoke.getDispatchReceiverParameter()!!.getType()))!!)
    }

    return result
}

private fun createSynthesizedFunctionWithFirstParameterAsReceiver(descriptor: FunctionDescriptor): FunctionDescriptor {
    val result = SimpleFunctionDescriptorImpl.create(
            descriptor.getContainingDeclaration(),
            descriptor.getAnnotations(),
            descriptor.getName(),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            descriptor.getSource()
    )

    val original = descriptor.getOriginal()
    result.initialize(
            original.getValueParameters().first().getType(),
            original.getDispatchReceiverParameter(),
            original.getTypeParameters(),
            original.getValueParameters().drop(1).map { p ->
                ValueParameterDescriptorImpl(
                        result, null, p.getIndex() - 1, p.getAnnotations(), Name.identifier("p${p.getIndex() + 1}"), p.getType(),
                        p.declaresDefaultValue(), p.getVarargElementType(), p.getSource()
                )
            },
            original.getReturnType(),
            original.getModality(),
            original.getVisibility()
    )
    result.isOperator = original.isOperator
    result.isInfix = original.isInfix

    return result
}

fun isSynthesizedInvoke(descriptor: DeclarationDescriptor): Boolean {
    if (descriptor.getName() != OperatorNameConventions.INVOKE || descriptor !is FunctionDescriptor) return false

    var real: FunctionDescriptor = descriptor
    while (!real.getKind().isReal()) {
        // You can't override two different synthesized invokes at the same time
        real = real.getOverriddenDescriptors().singleOrNull() ?: return false
    }

    return real.getKind() == CallableMemberDescriptor.Kind.SYNTHESIZED &&
           real.getContainingDeclaration() is FunctionClassDescriptor
}
