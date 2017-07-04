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
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.ConstantValueFactory
import org.jetbrains.kotlin.serialization.ProtoBuf.Annotation
import org.jetbrains.kotlin.serialization.ProtoBuf.Annotation.Argument
import org.jetbrains.kotlin.serialization.ProtoBuf.Annotation.Argument.Value
import org.jetbrains.kotlin.serialization.ProtoBuf.Annotation.Argument.Value.Type
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class AnnotationDeserializer(private val module: ModuleDescriptor, private val notFoundClasses: NotFoundClasses) {
    private val builtIns: KotlinBuiltIns
        get() = module.builtIns

    private val factory = ConstantValueFactory(builtIns)

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
        return Pair(nameResolver.getName(proto.nameId), resolveValue(parameter.type, proto.value, nameResolver))
    }

    fun resolveValue(
            expectedType: KotlinType,
            value: Value,
            nameResolver: NameResolver
    ): ConstantValue<*> {
        val result: ConstantValue<*> = when (value.type) {
            Type.BYTE -> factory.createByteValue(value.intValue.toByte())
            Type.CHAR -> factory.createCharValue(value.intValue.toChar())
            Type.SHORT -> factory.createShortValue(value.intValue.toShort())
            Type.INT -> factory.createIntValue(value.intValue.toInt())
            Type.LONG -> factory.createLongValue(value.intValue)
            Type.FLOAT -> factory.createFloatValue(value.floatValue)
            Type.DOUBLE -> factory.createDoubleValue(value.doubleValue)
            Type.BOOLEAN -> factory.createBooleanValue(value.intValue != 0L)
            Type.STRING -> {
                factory.createStringValue(nameResolver.getString(value.stringValue))
            }
            Type.CLASS -> {
                // TODO: support class literals
                error("Class literal annotation arguments are not supported yet (${nameResolver.getClassId(value.classId)})")
            }
            Type.ENUM -> {
                resolveEnumValue(nameResolver.getClassId(value.classId), nameResolver.getName(value.enumValueId))
            }
            Type.ANNOTATION -> {
                AnnotationValue(deserializeAnnotation(value.annotation, nameResolver))
            }
            Type.ARRAY -> {
                val expectedIsArray = KotlinBuiltIns.isArray(expectedType) || KotlinBuiltIns.isPrimitiveArray(expectedType)
                val arrayElements = value.arrayElementList

                val actualArrayType =
                        if (arrayElements.isNotEmpty()) {
                            val actualElementType = resolveArrayElementType(arrayElements.first(), nameResolver)
                            builtIns.getPrimitiveArrayKotlinTypeByPrimitiveKotlinType(actualElementType) ?:
                            builtIns.getArrayType(Variance.INVARIANT, actualElementType)
                        }
                        else {
                            // In the case of empty array, no element has the element type, so we fall back to the expected type, if any.
                            // This is not very accurate when annotation class has been changed without recompiling clients,
                            // but should not in fact matter because the value is empty anyway
                            if (expectedIsArray) expectedType else builtIns.getArrayType(Variance.INVARIANT, builtIns.anyType)
                        }

                val expectedElementType = builtIns.getArrayElementType(if (expectedIsArray) expectedType else actualArrayType)

                factory.createArrayValue(
                        arrayElements.map {
                            resolveValue(expectedElementType, it, nameResolver)
                        },
                        actualArrayType
                )
            }
            else -> error("Unsupported annotation argument type: ${value.type} (expected $expectedType)")
        }

        return if (result.type.isSubtypeOf(expectedType)) {
            result
        }
        else {
            // This means that an annotation class has been changed incompatibly without recompiling clients
            factory.createErrorValue("Unexpected argument value")
        }
    }

    // NOTE: see analogous code in BinaryClassAnnotationAndConstantLoaderImpl
    private fun resolveEnumValue(enumClassId: ClassId, enumEntryName: Name): ConstantValue<*> {
        val enumClass = resolveClass(enumClassId)
        if (enumClass.kind == ClassKind.ENUM_CLASS) {
            val enumEntry = enumClass.unsubstitutedInnerClassesScope.getContributedClassifier(enumEntryName, NoLookupLocation.FROM_DESERIALIZATION)
            if (enumEntry is ClassDescriptor) {
                return factory.createEnumValue(enumEntry)
            }
        }
        return factory.createErrorValue("Unresolved enum entry: $enumClassId.$enumEntryName")
    }

    private fun resolveArrayElementType(value: Value, nameResolver: NameResolver): SimpleType =
            with(builtIns) {
                when (value.type) {
                    Type.BYTE -> byteType
                    Type.CHAR -> charType
                    Type.SHORT -> shortType
                    Type.INT -> intType
                    Type.LONG -> longType
                    Type.FLOAT -> floatType
                    Type.DOUBLE -> doubleType
                    Type.BOOLEAN -> booleanType
                    Type.STRING -> stringType
                    Type.CLASS -> error("Arrays of class literals are not supported yet") // TODO: support arrays of class literals
                    Type.ENUM -> resolveClass(nameResolver.getClassId(value.classId)).defaultType
                    Type.ANNOTATION -> resolveClass(nameResolver.getClassId(value.annotation.id)).defaultType
                    Type.ARRAY -> error("Array of arrays is impossible")
                    else -> error("Unknown type: ${value.type}")
                }
            }

    private fun resolveClass(classId: ClassId): ClassDescriptor {
        return module.findNonGenericClassAcrossDependencies(classId, notFoundClasses)
    }
}
