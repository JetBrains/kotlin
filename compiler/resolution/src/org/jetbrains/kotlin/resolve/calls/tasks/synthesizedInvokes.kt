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

package org.jetbrains.kotlin.resolve.calls.tasks

import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor.Kind.Function
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor.Kind.SuspendFunction
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.setSingleOverridden
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.*

private val BUILTIN_FUNCTIONS = setOf(Function, SuspendFunction)

fun createSynthesizedInvokes(functions: Collection<FunctionDescriptor>): Collection<FunctionDescriptor> {
    val result = ArrayList<FunctionDescriptor>(1)

    for (invoke in functions) {
        if (invoke !is FunctionInvokeDescriptor || invoke.getValueParameters().isEmpty()) continue

        val synthesized = if ((invoke.getContainingDeclaration() as? FunctionClassDescriptor)?.functionKind in BUILTIN_FUNCTIONS) {
            createSynthesizedFunctionWithFirstParameterAsReceiver(invoke)
        }
        else {
            val invokeDeclaration = invoke.getOverriddenDescriptors().single()
            val synthesizedSuperFun = createSynthesizedFunctionWithFirstParameterAsReceiver(invokeDeclaration)
            val fakeOverride = synthesizedSuperFun.copy(
                    invoke.getContainingDeclaration(),
                    synthesizedSuperFun.modality,
                    synthesizedSuperFun.visibility,
                    CallableMemberDescriptor.Kind.FAKE_OVERRIDE,
                    /* copyOverrides = */ false
            )
            fakeOverride.setSingleOverridden(synthesizedSuperFun)
            fakeOverride
        }

        result.add(synthesized.substitute(TypeSubstitutor.create(invoke.getDispatchReceiverParameter()!!.type)) ?: continue)
    }

    return result
}

private fun createSynthesizedFunctionWithFirstParameterAsReceiver(descriptor: FunctionDescriptor) =
    descriptor.original.newCopyBuilder().apply {
        setExtensionReceiverType(descriptor.original.valueParameters.first().type)
        setValueParameters(
                descriptor.original.valueParameters
                        .drop(1)
                        .map { p -> p.copy(descriptor.original, Name.identifier("p${p.index + 1}"), p.index - 1) }
        )
    }.build()!!

fun isSynthesizedInvoke(descriptor: DeclarationDescriptor): Boolean {
    if (descriptor.name != OperatorNameConventions.INVOKE || descriptor !is FunctionDescriptor) return false

    var real: FunctionDescriptor = descriptor
    while (!real.kind.isReal) {
        // You can't override two different synthesized invokes at the same time
        real = real.overriddenDescriptors.singleOrNull() ?: return false
    }

    return real.kind == CallableMemberDescriptor.Kind.SYNTHESIZED &&
           real.containingDeclaration is FunctionClassDescriptor
}
