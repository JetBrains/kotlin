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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.descriptors.JavaConstructorDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.structure.reflect.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import kotlin.reflect.KotlinReflectionInternalError

internal sealed class JvmFunctionSignature {
    abstract fun asString(): String

    class KotlinFunction(
            val proto: ProtoBuf.Callable,
            val signature: JvmProtoBuf.JvmMethodSignature,
            val nameResolver: NameResolver
    ) : JvmFunctionSignature() {
        override fun asString(): String =
                nameResolver.getString(signature.name) + nameResolver.getString(signature.desc)
    }

    class JavaMethod(val method: Method) : JvmFunctionSignature() {
        override fun asString(): String =
                method.name +
                method.parameterTypes.joinToString(separator = "", prefix = "(", postfix = ")") { it.desc } +
                method.returnType.desc
    }

    class JavaConstructor(val constructor: Constructor<*>) : JvmFunctionSignature() {
        override fun asString(): String =
                "<init>" +
                constructor.parameterTypes.joinToString(separator = "", prefix = "(", postfix = ")") { it.desc } +
                "V"
    }

    open class BuiltInFunction(private val signature: String) : JvmFunctionSignature() {
        open fun getMember(container: KDeclarationContainerImpl): Member? = null

        override fun asString(): String = signature

        class Predefined(signature: String, private val member: Member): BuiltInFunction(signature) {
            override fun getMember(container: KDeclarationContainerImpl): Member = member
        }
    }
}

internal sealed class JvmPropertySignature {
    /**
     * Returns the JVM signature of the getter of this property. In case the property doesn't have a getter,
     * constructs the signature of its imaginary default getter. See CallableReference#getSignature for more information
     */
    abstract fun asString(): String

    class KotlinProperty(
            val proto: ProtoBuf.Callable,
            val signature: JvmProtoBuf.JvmPropertySignature,
            val nameResolver: NameResolver
    ) : JvmPropertySignature() {
        private val string: String

        init {
            if (signature.hasGetter()) {
                string = nameResolver.getString(signature.getter.name) + nameResolver.getString(signature.getter.desc)
            }
            else {
                string = JvmAbi.getterName(nameResolver.getString(signature.field.name)) +
                         "()" +
                         nameResolver.getString(signature.field.desc)
            }
        }

        override fun asString(): String = string
    }

    class JavaField(val field: Field) : JvmPropertySignature() {
        override fun asString(): String =
                JvmAbi.getterName(field.name) +
                "()" +
                field.type.desc
    }
}

internal object RuntimeTypeMapper {
    fun mapSignature(possiblySubstitutedFunction: FunctionDescriptor): JvmFunctionSignature {
        // Fake overrides don't have a source element, so we need to take a declaration.
        // TODO: support the case when a fake override overrides several declarations with different signatures
        val function = DescriptorUtils.unwrapFakeOverride(possiblySubstitutedFunction).original

        when (function) {
            is DeserializedCallableMemberDescriptor -> {
                val proto = function.proto
                if (proto.hasExtension(JvmProtoBuf.methodSignature)) {
                    val signature = proto.getExtension(JvmProtoBuf.methodSignature)
                    return JvmFunctionSignature.KotlinFunction(proto, signature, function.nameResolver)
                }
                // If it's a deserialized function but has no JVM signature, it must be from built-ins
                return mapIntrinsicFunctionSignature(function) ?:
                       throw KotlinReflectionInternalError("Reflection on built-in Kotlin types is not yet fully supported. " +
                                                           "No metadata found for $function")
            }
            is JavaMethodDescriptor -> {
                val method = ((function.source as? JavaSourceElement)?.javaElement as? ReflectJavaMethod)?.member ?:
                             throw KotlinReflectionInternalError("Incorrect resolution sequence for Java method $function")

                return JvmFunctionSignature.JavaMethod(method)
            }
            is JavaConstructorDescriptor -> {
                val constructor = ((function.source as? JavaSourceElement)?.javaElement as? ReflectJavaConstructor)?.member ?:
                                  throw KotlinReflectionInternalError("Incorrect resolution sequence for Java constructor $function")

                return JvmFunctionSignature.JavaConstructor(constructor)
            }
            else -> throw KotlinReflectionInternalError("Unknown origin of $function (${function.javaClass})")
        }
    }

    fun mapPropertySignature(possiblyOverriddenProperty: PropertyDescriptor): JvmPropertySignature {
        val property = DescriptorUtils.unwrapFakeOverride(possiblyOverriddenProperty)
        if (property is DeserializedPropertyDescriptor) {
            val proto = property.proto
            if (!proto.hasExtension(JvmProtoBuf.propertySignature)) {
                throw KotlinReflectionInternalError("No metadata found for $property")
            }
            return JvmPropertySignature.KotlinProperty(proto, proto.getExtension(JvmProtoBuf.propertySignature), property.nameResolver)
        }
        else if (property is JavaPropertyDescriptor) {
            val field = ((property.source as? JavaSourceElement)?.javaElement as? ReflectJavaField)?.member ?:
                         throw KotlinReflectionInternalError("Incorrect resolution sequence for Java field $property")

            return JvmPropertySignature.JavaField(field)
        }
        else throw KotlinReflectionInternalError("Unknown origin of $property (${property.javaClass})")
    }

    private fun mapIntrinsicFunctionSignature(function: FunctionDescriptor): JvmFunctionSignature? {
        val parameters = function.valueParameters

        when (function.name.asString()) {
            "equals" -> if (parameters.size() == 1 && KotlinBuiltIns.isNullableAny(parameters.single().type)) {
                return JvmFunctionSignature.BuiltInFunction.Predefined("equals(Ljava/lang/Object;)Z",
                                                                       Any::class.java.getDeclaredMethod("equals", Any::class.java))
            }
            "hashCode" -> if (parameters.isEmpty()) {
                return JvmFunctionSignature.BuiltInFunction.Predefined("hashCode()I",
                                                                       Any::class.java.getDeclaredMethod("hashCode"))
            }
            "toString" -> if (parameters.isEmpty()) {
                return JvmFunctionSignature.BuiltInFunction.Predefined("toString()Ljava/lang/String;",
                                                                       Any::class.java.getDeclaredMethod("toString"))
            }
            // TODO: generalize and support other functions from built-ins
        }

        return null
    }

    fun mapJvmClassToKotlinClassId(klass: Class<*>): ClassId {
        if (klass.isArray()) {
            klass.getComponentType().primitiveType?.let {
                return KotlinBuiltIns.getInstance().getPrimitiveArrayClassDescriptor(it).classId
            }
            return ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.array.toSafe())
        }

        klass.primitiveType?.let {
            return KotlinBuiltIns.getInstance().getPrimitiveClassDescriptor(it).classId
        }

        val classId = klass.classId
        if (!classId.isLocal()) {
            JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(classId.asSingleFqName())?.let { return it.classId }
        }

        return classId
    }

    private val Class<*>.primitiveType: PrimitiveType?
        get() = if (isPrimitive()) JvmPrimitiveType.get(getSimpleName()).getPrimitiveType() else null
}
