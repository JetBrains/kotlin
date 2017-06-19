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
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import java.lang.reflect.Type
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.javaType

internal abstract class KCallableImpl<out R> : KCallable<R> {
    abstract val descriptor: CallableMemberDescriptor

    // The instance which is used to perform a positional call, i.e. `call`
    abstract val caller: FunctionCaller<*>

    // The instance which is used to perform a call "by name", i.e. `callBy`
    abstract val defaultCaller: FunctionCaller<*>?

    abstract val container: KDeclarationContainerImpl

    abstract val isBound: Boolean

    private val annotations_ = ReflectProperties.lazySoft { descriptor.computeAnnotations() }

    override val annotations: List<Annotation> get() = annotations_()

    private val parameters_ = ReflectProperties.lazySoft {
        val descriptor = descriptor
        val result = ArrayList<KParameter>()
        var index = 0

        if (descriptor.dispatchReceiverParameter != null && !isBound) {
            result.add(KParameterImpl(this, index++, KParameter.Kind.INSTANCE) { descriptor.dispatchReceiverParameter!! })
        }

        if (descriptor.extensionReceiverParameter != null && !isBound) {
            result.add(KParameterImpl(this, index++, KParameter.Kind.EXTENSION_RECEIVER) { descriptor.extensionReceiverParameter!! })
        }

        for (i in descriptor.valueParameters.indices) {
            result.add(KParameterImpl(this, index++, KParameter.Kind.VALUE) { descriptor.valueParameters[i] })
        }

        // Constructor parameters of Java annotations are not ordered in any way, we order them by name here to be more stable.
        // Note that positional call (via "call") is not allowed unless there's a single non-"value" parameter,
        // so the order of parameters of Java annotation constructors here can be arbitrary
        if (isAnnotationConstructor && descriptor is JavaCallableMemberDescriptor) {
            result.sortBy { it.name }
        }

        result.trimToSize()
        result
    }

    override val parameters: List<KParameter>
        get() = parameters_()

    private val returnType_ = ReflectProperties.lazySoft {
        KTypeImpl(descriptor.returnType!!) { caller.returnType }
    }

    override val returnType: KType
        get() = returnType_()

    private val typeParameters_ = ReflectProperties.lazySoft {
        descriptor.typeParameters.map(::KTypeParameterImpl)
    }

    override val typeParameters: List<KTypeParameter>
        get() = typeParameters_()

    override val visibility: KVisibility?
        get() = descriptor.visibility.toKVisibility()

    override val isFinal: Boolean
        get() = descriptor.modality == Modality.FINAL

    override val isOpen: Boolean
        get() = descriptor.modality == Modality.OPEN

    override val isAbstract: Boolean
        get() = descriptor.modality == Modality.ABSTRACT

    protected val isAnnotationConstructor: Boolean
        get() = name == "<init>" && container.jClass.isAnnotation

    @Suppress("UNCHECKED_CAST")
    override fun call(vararg args: Any?): R = reflectionCall {
        return caller.call(args) as R
    }

    override fun callBy(args: Map<KParameter, Any?>): R {
        return if (isAnnotationConstructor) callAnnotationConstructor(args) else callDefaultMethod(args)
    }

    // See ArgumentGenerator#generate
    private fun callDefaultMethod(args: Map<KParameter, Any?>): R {
        val parameters = parameters
        val arguments = ArrayList<Any?>(parameters.size)
        var mask = 0
        val masks = ArrayList<Int>(1)
        var index = 0
        var anyOptional = false

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
                    anyOptional = true
                }
                else -> {
                    throw IllegalArgumentException("No argument provided for a required parameter: $parameter")
                }
            }

            if (parameter.kind == KParameter.Kind.VALUE) {
                index++
            }
        }

        if (!anyOptional) {
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

    private fun callAnnotationConstructor(args: Map<KParameter, Any?>): R {
        val arguments = parameters.map { parameter ->
            when {
                args.containsKey(parameter) -> {
                    args[parameter] ?: throw IllegalArgumentException("Annotation argument value cannot be null ($parameter)")
                }
                parameter.isOptional -> null
                else -> throw IllegalArgumentException("No argument provided for a required parameter: $parameter")
            }
        }

        val caller = defaultCaller ?: throw KotlinReflectionInternalError("This callable does not support a default call: $descriptor")

        @Suppress("UNCHECKED_CAST")
        return reflectionCall {
            caller.call(arguments.toTypedArray()) as R
        }
    }

    private fun defaultPrimitiveValue(type: Type): Any? =
            if (type is Class<*> && type.isPrimitive) {
                when (type) {
                    Boolean::class.java -> false
                    Char::class.java -> 0.toChar()
                    Byte::class.java -> 0.toByte()
                    Short::class.java -> 0.toShort()
                    Int::class.java -> 0
                    Float::class.java -> 0f
                    Long::class.java -> 0L
                    Double::class.java -> 0.0
                    Void.TYPE -> throw IllegalStateException("Parameter with void type is illegal")
                    else -> throw UnsupportedOperationException("Unknown primitive: $type")
                }
            }
            else null
}
