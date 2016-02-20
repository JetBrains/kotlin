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

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import java.lang.reflect.Type
import java.util.*
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KotlinReflectionInternalError
import kotlin.reflect.jvm.javaType

internal interface KCallableImpl<out R> : KCallable<R>, KAnnotatedElementImpl {
    val descriptor: CallableMemberDescriptor

    val caller: FunctionCaller<*>

    val defaultCaller: FunctionCaller<*>?

    val container: KDeclarationContainerImpl

    override val annotated: Annotated get() = descriptor

    override val parameters: List<KParameter>
        get() {
            val descriptor = descriptor
            val result = ArrayList<KParameter>()
            var index = 0

            if (descriptor.dispatchReceiverParameter != null) {
                result.add(KParameterImpl(this, index++, KParameter.Kind.INSTANCE) { descriptor.dispatchReceiverParameter!! })
            }

            if (descriptor.extensionReceiverParameter != null) {
                result.add(KParameterImpl(this, index++, KParameter.Kind.EXTENSION_RECEIVER) { descriptor.extensionReceiverParameter!! })
            }

            for (i in descriptor.valueParameters.indices) {
                result.add(KParameterImpl(this, index++, KParameter.Kind.VALUE) { descriptor.valueParameters[i] })
            }

            result.trimToSize()
            return result
        }

    override val returnType: KType
        get() = KTypeImpl(descriptor.returnType!!) { caller.returnType }

    @Suppress("UNCHECKED_CAST")
    override fun call(vararg args: Any?): R = reflectionCall {
        return caller.call(args) as R
    }

    // See ArgumentGenerator#generate
    override fun callBy(args: Map<KParameter, Any?>): R {
        val parameters = parameters
        val arguments = ArrayList<Any?>(parameters.size)
        var mask = 0
        val masks = ArrayList<Int>(1)
        var index = 0

        for (parameter in parameters) {
            if (index != 0 && index % Integer.SIZE == 0) {
                masks.add(mask)
                mask = 0
            }

            when {
                args.containsKey(parameter) -> {
                    arguments.add(args[parameter])
                }
                parameter.isOptional -> {
                    arguments.add(defaultPrimitiveValue(parameter.type.javaType))
                    mask = mask or (1 shl (index % Integer.SIZE))
                }
                else -> {
                    throw IllegalArgumentException("No argument provided for a required parameter: $parameter")
                }
            }

            if (parameter.kind == KParameter.Kind.VALUE) {
                index++
            }
        }

        if (mask == 0 && masks.isEmpty()) {
            return call(*arguments.toTypedArray())
        }

        masks.add(mask)

        val caller = defaultCaller ?: throw KotlinReflectionInternalError("This callable does not support a default call: $descriptor")

        arguments.addAll(masks)

        // DefaultConstructorMarker or MethodHandle
        arguments.add(null)

        @Suppress("UNCHECKED_CAST")
        return reflectionCall {
            caller.call(arguments.toTypedArray()) as R
        }
    }

    private fun defaultPrimitiveValue(type: Type): Any? =
            if (type is Class<*> && type.isPrimitive) {
                when (type) {
                    java.lang.Boolean.TYPE -> false
                    java.lang.Character.TYPE -> 0.toChar()
                    java.lang.Byte.TYPE -> 0.toByte()
                    java.lang.Short.TYPE -> 0.toShort()
                    java.lang.Integer.TYPE -> 0
                    java.lang.Float.TYPE -> 0f
                    java.lang.Long.TYPE -> 0L
                    java.lang.Double.TYPE -> 0.0
                    java.lang.Void.TYPE -> throw IllegalStateException("Parameter with void type is illegal")
                    else -> throw UnsupportedOperationException("Unknown primitive: $type")
                }
            }
            else null
}
