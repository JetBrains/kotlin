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

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.render
import kotlin.reflect.*
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.valueParameters

internal object ReflectionObjectRenderer {
    private val renderer = DescriptorRenderer.FQ_NAMES_IN_TYPES

    private fun StringBuilder.appendReceiverType(receiver: KParameter): StringBuilder =
        append(renderType(receiver.type)).append(".")

    private fun StringBuilder.appendReceivers(callable: KCallable<*>) {
        val receivers = (callable as KCallableImpl<*>).receiverParameters.filter {
            it.kind == KParameter.Kind.INSTANCE || it.kind == KParameter.Kind.EXTENSION_RECEIVER
        }
        receivers.getOrNull(0)?.let { appendReceiverType(it) }
        receivers.getOrNull(1)?.let { append("(").appendReceiverType(it).append(")") }
    }

    private fun StringBuilder.appendName(name: String) {
        append(Name.identifier(name).render())
    }

    private fun renderCallable(callable: KCallable<*>): String {
        return when (callable) {
            is KProperty<*> -> renderProperty(callable)
            is KFunction<*> -> renderFunction(callable)
            else -> error("Illegal callable: $callable")
        }
    }

    // TODO: include visibility
    fun renderProperty(property: KProperty<*>): String {
        return buildString {
            append(if (property is KMutableProperty<*>) "var " else "val ")
            appendReceivers(property)
            appendName(property.name)

            append(": ")
            append(renderType(property.returnType))
        }
    }

    fun renderFunction(function: KFunction<*>): String {
        return buildString {
            append("fun ")
            appendReceivers(function)
            appendName(function.name)

            function.valueParameters.joinTo(this, separator = ", ", prefix = "(", postfix = ")") {
                renderType(it.type) // TODO: vararg
            }

            append(": ")
            append(renderType(function.returnType))
        }
    }

    fun renderLambda(lambda: KFunction<*>): String {
        return buildString {
            lambda.extensionReceiverParameter?.let {
                append(renderType(it.type))
                append(".")
            }

            lambda.valueParameters.joinTo(this, separator = ", ", prefix = "(", postfix = ")") {
                renderType(it.type)
            }

            append(" -> ")
            append(renderType(lambda.returnType))
        }
    }

    fun renderParameter(parameter: KParameter): String {
        return buildString {
            when (parameter.kind) {
                KParameter.Kind.INSTANCE -> append("instance parameter")
                @OptIn(ExperimentalContextParameters::class)
                KParameter.Kind.CONTEXT,
                    -> append("context parameter ${parameter.name}")
                KParameter.Kind.EXTENSION_RECEIVER -> append("extension receiver parameter")
                KParameter.Kind.VALUE -> append("parameter #${parameter.index} ${parameter.name}")
            }

            append(" of ")
            append(renderCallable((parameter as KParameterImpl).callable))
        }
    }

    fun renderType(type: KType): String {
        // TODO: implement toString without dependency on descriptors
        return renderer.renderType((type as KTypeImpl).type)
    }
}
