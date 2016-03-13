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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.KotlinType
import kotlin.reflect.KParameter

internal object ReflectionObjectRenderer {
    private val renderer = DescriptorRenderer.FQ_NAMES_IN_TYPES

    private fun StringBuilder.appendReceiverType(receiver: ReceiverParameterDescriptor?) {
        if (receiver != null) {
            append(renderType(receiver.type))
            append(".")
        }
    }

    private fun StringBuilder.appendReceivers(callable: CallableDescriptor) {
        val dispatchReceiver = callable.dispatchReceiverParameter
        val extensionReceiver = callable.extensionReceiverParameter

        appendReceiverType(dispatchReceiver)

        val addParentheses = dispatchReceiver != null && extensionReceiver != null
        if (addParentheses) append("(")
        appendReceiverType(extensionReceiver)
        if (addParentheses) append(")")
    }

    fun renderCallable(descriptor: CallableDescriptor): String {
        return when (descriptor) {
            is PropertyDescriptor -> renderProperty(descriptor)
            is FunctionDescriptor -> renderFunction(descriptor)
            else -> error("Illegal callable: $descriptor")
        }
    }

    // TODO: include visibility
    fun renderProperty(descriptor: PropertyDescriptor): String {
        return buildString {
            append(if (descriptor.isVar) "var " else "val ")
            appendReceivers(descriptor)
            append(renderer.renderName(descriptor.name))

            append(": ")
            append(renderType(descriptor.type))
        }
    }

    fun renderFunction(descriptor: FunctionDescriptor): String {
        return buildString {
            append("fun ")
            appendReceivers(descriptor)
            append(renderer.renderName(descriptor.name))

            descriptor.valueParameters.joinTo(this, separator = ", ", prefix = "(", postfix = ")") {
                renderType(it.type) // TODO: vararg
            }

            append(": ")
            append(renderType(descriptor.returnType!!))
        }
    }

    fun renderLambda(invoke: FunctionDescriptor): String {
        return buildString {
            appendReceivers(invoke)

            invoke.valueParameters.joinTo(this, separator = ", ", prefix = "(", postfix = ")") {
                renderType(it.type)
            }

            append(" -> ")
            append(renderType(invoke.returnType!!))
        }
    }

    fun renderParameter(parameter: KParameterImpl): String {
        return buildString {
            when (parameter.kind) {
                KParameter.Kind.EXTENSION_RECEIVER -> append("extension receiver")
                KParameter.Kind.INSTANCE -> append("instance")
                KParameter.Kind.VALUE -> append("parameter #${parameter.index} ${parameter.name}")
            }

            append(" of ")
            append(renderCallable(parameter.callable.descriptor))
        }
    }

    fun renderType(type: KotlinType): String {
        return renderer.renderType(type)
    }
}
