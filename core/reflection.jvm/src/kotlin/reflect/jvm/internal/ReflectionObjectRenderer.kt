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

object ReflectionObjectRenderer {
    private val renderer = DescriptorRenderer.FQ_NAMES_IN_TYPES

    private fun StringBuilder.appendReceiverType(receiver: ReceiverParameterDescriptor?) {
        if (receiver != null) {
            append(renderer.renderType(receiver.getType()))
            append(".")
        }
    }

    private fun StringBuilder.appendReceiversAndName(callable: CallableDescriptor) {
        val dispatchReceiver = callable.getDispatchReceiverParameter()
        val extensionReceiver = callable.getExtensionReceiverParameter()

        appendReceiverType(dispatchReceiver)

        val addParentheses = dispatchReceiver != null && extensionReceiver != null
        if (addParentheses) append("(")
        appendReceiverType(extensionReceiver)
        if (addParentheses) append(")")

        append(renderer.renderName(callable.getName()))
    }

    // TODO: include visibility, return type
    fun renderProperty(descriptor: PropertyDescriptor): String {
        return StringBuilder {
            append(if (descriptor.isVar()) "var " else "val ")
            appendReceiversAndName(descriptor)
        }.toString()
    }

    fun renderFunction(descriptor: FunctionDescriptor): String {
        // TODO: add tests
        return StringBuilder {
            append("fun ")
            appendReceiversAndName(descriptor)

            descriptor.getValueParameters().joinTo(this, separator = ", ", prefix = "(", postfix = ")") {
                renderer.renderType(it.getType()) // TODO: vararg
            }

            append(": ")
            append(renderer.renderType(descriptor.getReturnType()!!))
        }.toString()
    }
}
