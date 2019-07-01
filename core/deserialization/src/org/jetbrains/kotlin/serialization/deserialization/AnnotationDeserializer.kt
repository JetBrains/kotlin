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

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType

class AnnotationDeserializer(private val module: ModuleDescriptor, private val notFoundClasses: NotFoundClasses) {
    private val builtIns: KotlinBuiltIns
        get() = module.builtIns

    fun deserializeAnnotation(proto: Annotation, nameResolver: NameResolver): AnnotationDescriptor {
        val annotationClass = resolveClass(nameResolver.getClassId(proto.id))

        var arguments = emptyMap<Name, ConstantValue<*>>()
        if (proto.argumentCount != 0 && !ErrorUtils.isError(annotationClass) && DescriptorUtils.isAnnotationClass(annotationClass)) {
            val constructor = annotationClass.constructors.singleOrNull()
            if (constructor != null) {
                val parameterByName = constructor.valueParameters.associateBy { it.name }
                arguments = proto.argumentList.mapNotNull { resolveArgument(it, parameterByName, nameResolver) }.toMap()
            }
        }

        return AnnotationDescriptorImpl(annotationClass.defaultType, arguments, SourceElement.NO_SOURCE)
    }

    private fun resolveArgument(
        proto: Argument,
        parameterByName: Map<Name, ValueParameterDescriptor>,
        nameResolver: NameResolver
    ): Pair<Name, ConstantValue<*>>? {
        val parameter = parameterByName[nameResolver.getName(proto.nameId)] ?: return null
        return Pair(nameResolver.getName(proto.nameId), resolveValueAndCheckExpectedType(parameter.type, proto.value, nameResolver))
    }

    private fun resolveValueAndCheckExpectedType(expectedType: KotlinType, value: Value, nameResolver: NameResolver): ConstantValue<*> {
        return resolveValue(expectedType, value, nameResolver).takeIf {
            doesValueConformToExpectedType(it, expectedType, value)
        } ?: ErrorValue.create("Unexpected argument value: actual type ${value.type} != expected type $expectedType")
    }

    fun resolveValue(expectedType: KotlinType, value: Value, nameResolver: NameResolver): ConstantValue<*> {
        val isUnsigned = Flags.IS_UNSIGNED.get(value.flags)

        return when (value.type) {
            Type.BYTE -> value.intValue.toByte().letIf(isUnsigned, ::UByteValue, ::ByteValue)
            Type.CHAR -> CharValue(value.intValue.toChar())
            Type.SHORT -> value.intValue.toShort().letIf(isUnsigned, ::UShortValue, ::ShortValue)
            Type.INT -> value.intValue.toInt().letIf(isUnsigned, ::UIntValue, ::IntValue)
            Type.LONG -> value.intValue.letIf(isUnsigned, ::ULongValue, ::LongValue)
            Type.FLOAT -> FloatValue(value.floatValue)
            Type.DOUBLE -> DoubleValue(value.doubleValue)
            Type.BOOLEAN -> BooleanValue(value.intValue != 0L)
            Type.STRING -> StringValue(nameResolver.getString(value.stringValue))
            Type.CLASS -> KClassValue(nameResolver.getClassId(value.classId), value.arrayDimensionCount)
            Type.ENUM -> EnumValue(nameResolver.getClassId(value.classId), nameResolver.getName(value.enumValueId))
            Type.ANNOTATION -> AnnotationValue(deserializeAnnotation(value.annotation, nameResolver))
            Type.ARRAY -> ConstantValueFactory.createArrayValue(
                value.arrayElementList.map { resolveValue(builtIns.anyType, it, nameResolver) },
                expectedType
            )
            else -> error("Unsupported annotation argument type: ${value.type} (expected $expectedType)")
        }
    }

    // This method returns false if the actual value loaded from an annotation argument does not conform to the expected type of the
    // corresponding parameter in the annotation class. This usually means that the annotation class has been changed incompatibly
    // without recompiling clients, in which case we prefer not to load the annotation argument value at all, to avoid constructing
    // an incorrect model and breaking some assumptions in the compiler.
    private fun doesValueConformToExpectedType(result: ConstantValue<*>, expectedType: KotlinType, value: Value): Boolean {
        return when (value.type) {
            Type.CLASS -> {
                val expectedClass = expectedType.constructor.declarationDescriptor as? ClassDescriptor
                // We could also check that the class value's type is a subtype of the expected type, but loading the definition of the
                // referenced class here is undesirable and may even be incorrect (because the module might be different at the
                // destination where these constant values are read). This can lead to slightly incorrect model in some edge cases.
                expectedClass == null || KotlinBuiltIns.isKClass(expectedClass)
            }
            Type.ARRAY -> {
                check(result is ArrayValue && result.value.size == value.arrayElementList.size) {
                    "Deserialized ArrayValue should have the same number of elements as the original array value: $result"
                }
                val expectedElementType = builtIns.getArrayElementType(expectedType)
                result.value.indices.all { i ->
                    doesValueConformToExpectedType(result.value[i], expectedElementType, value.getArrayElement(i))
                }
            }
            else -> result.getType(module) == expectedType
        }
    }

    private inline fun <T, R> T.letIf(predicate: Boolean, f: (T) -> R, g: (T) -> R): R =
        if (predicate) f(this) else g(this)

    private fun resolveClass(classId: ClassId): ClassDescriptor {
        return module.findNonGenericClassAcrossDependencies(classId, notFoundClasses)
    }
}
