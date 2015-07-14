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
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.reflect.*
import org.jetbrains.kotlin.load.kotlin.SignatureDeserializer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import kotlin.reflect.KotlinReflectionInternalError

object RuntimeTypeMapper {
    fun mapSignature(possiblySubstitutedFunction: FunctionDescriptor): String {
        val function = possiblySubstitutedFunction.getOriginal()

        if (function is DeserializedCallableMemberDescriptor) {
            val proto = function.proto
            if (!proto.hasExtension(JvmProtoBuf.methodSignature)) {
                // If it's a deserialized function but has no JVM signature, it must be from built-ins
                return mapIntrinsicFunctionSignature(function) ?:
                       throw KotlinReflectionInternalError("No metadata found for $function")
            }
            val signature = proto.getExtension(JvmProtoBuf.methodSignature)
            return SignatureDeserializer(function.nameResolver).methodSignatureString(signature)
        }
        else if (function is JavaMethodDescriptor) {
            val method = (function.getSource() as? JavaSourceElement)?.javaElement as? JavaMethod ?:
                         throw KotlinReflectionInternalError("Incorrect resolution sequence for Java method $function")

            return StringBuilder {
                append(method.getName().asString())

                append("(")
                for (parameter in method.getValueParameters()) {
                    appendJavaType(parameter.getType())
                }
                append(")")

                appendJavaType(method.getReturnType())
            }.toString()
        }
        else throw KotlinReflectionInternalError("Unknown origin of $function (${function.javaClass})")
    }

    // TODO: verify edge cases when it's possible to reference generic functions
    private tailRecursive fun StringBuilder.appendJavaType(type: JavaType) {
        when (type) {
            is JavaPrimitiveType -> {
                append(type.getType()?.let { JvmPrimitiveType.get(it).getDesc() } ?: "V")
            }
            is JavaArrayType -> {
                append("[")
                appendJavaType(type.getComponentType())
            }
            is JavaWildcardType -> {
                val bound = type.getBound()
                if (bound != null && type.isExtends()) appendJavaType(bound)
                else append("Ljava/lang/Object;")
            }
            is JavaClassifierType -> {
                val classifier = type.getClassifier()
                when (classifier) {
                    is ReflectJavaClass ->
                        append(classifier.element.desc)
                    is ReflectJavaTypeParameter ->
                        appendJavaType(ReflectJavaType.create(classifier.typeVariable.getBounds().first()))
                }
            }
        }
    }

    fun mapPropertySignature(property: PropertyDescriptor): String {
        if (property is DeserializedPropertyDescriptor) {
            val proto = property.proto
            val nameResolver = property.nameResolver
            if (!proto.hasExtension(JvmProtoBuf.propertySignature)) {
                throw KotlinReflectionInternalError("No metadata found for $property")
            }
            val signature = proto.getExtension(JvmProtoBuf.propertySignature)
            val deserializer = SignatureDeserializer(nameResolver)

            if (signature.hasGetter()) {
                return deserializer.methodSignatureString(signature.getGetter())
            }

            // In case the property doesn't have a getter, construct the signature of its imaginary default getter.
            // See PropertyReference#getSignature
            val field = signature.getField()

            // TODO: some kind of test on the Java Bean convention?
            return JvmAbi.getterName(nameResolver.getString(field.getName())) +
                   "()" +
                   deserializer.typeDescriptor(field.getType())
        }
        else if (property is JavaPropertyDescriptor) {
            val method = (property.getSource() as? JavaSourceElement)?.javaElement as? JavaField ?:
                         throw KotlinReflectionInternalError("Incorrect resolution sequence for Java field $property")

            return StringBuilder {
                append(JvmAbi.getterName(method.getName().asString()))
                append("()")
                appendJavaType(method.getType())
            }.toString()
        }
        else throw KotlinReflectionInternalError("Unknown origin of $property (${property.javaClass})")
    }

    private fun mapIntrinsicFunctionSignature(function: FunctionDescriptor): String? {
        val parameters = function.getValueParameters()

        when (function.getName().asString()) {
            "equals" -> {
                if (parameters.size() == 1 && KotlinBuiltIns.isNullableAny(parameters.single().getType())) {
                    return "equals(Ljava/lang/Object;)Z"
                }
            }
            "hashCode" -> {
                if (parameters.isEmpty()) {
                    return "hashCode()I"
                }
            }
            "toString" -> {
                if (parameters.isEmpty()) {
                    return "toString()Ljava/lang/String;"
                }
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
