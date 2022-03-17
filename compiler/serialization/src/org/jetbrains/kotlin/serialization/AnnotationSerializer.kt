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

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationArgumentVisitor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType

open class AnnotationSerializer(private val stringTable: DescriptorAwareStringTable) {
    fun serializeAnnotation(annotation: AnnotationDescriptor): ProtoBuf.Annotation? = ProtoBuf.Annotation.newBuilder().apply {
        val annotationClass = annotation.annotationClass ?: error("Annotation type is not a class: ${annotation.type}")
        if (ErrorUtils.isError(annotationClass)) {
            if (ignoreAnnotation(annotation.type)) return null
            error("Unresolved annotation type: ${annotation.type} at ${annotation.source.containingFile}")
        }

        id = stringTable.getFqNameIndex(annotationClass)

        for ((name, value) in annotation.allValueArguments) {
            val argument = ProtoBuf.Annotation.Argument.newBuilder()
            argument.nameId = stringTable.getStringIndex(name.asString())
            argument.setValue(valueProto(value))
            addArgument(argument)
        }
    }.build()

    fun valueProto(constant: ConstantValue<*>): Value.Builder = Value.newBuilder().apply {
        constant.accept(object : AnnotationArgumentVisitor<Unit, Unit> {
            override fun visitAnnotationValue(value: AnnotationValue, data: Unit) {
                type = Type.ANNOTATION
                annotation = serializeAnnotation(value.value)
            }

            override fun visitArrayValue(value: ArrayValue, data: Unit) {
                type = Type.ARRAY
                for (element in value.value) {
                    addArrayElement(valueProto(element).build())
                }
            }

            override fun visitBooleanValue(value: BooleanValue, data: Unit) {
                type = Type.BOOLEAN
                intValue = if (value.value) 1 else 0
            }

            override fun visitByteValue(value: ByteValue, data: Unit) {
                type = Type.BYTE
                intValue = value.value.toLong()
            }

            override fun visitCharValue(value: CharValue, data: Unit) {
                type = Type.CHAR
                intValue = value.value.code.toLong()
            }

            override fun visitDoubleValue(value: DoubleValue, data: Unit) {
                type = Type.DOUBLE
                doubleValue = value.value
            }

            override fun visitEnumValue(value: EnumValue, data: Unit) {
                type = Type.ENUM
                classId = stringTable.getQualifiedClassNameIndex(value.enumClassId)
                enumValueId = stringTable.getStringIndex(value.enumEntryName.asString())
            }

            override fun visitErrorValue(value: ErrorValue, data: Unit) {
                throw UnsupportedOperationException("Error value: $value")
            }

            override fun visitFloatValue(value: FloatValue, data: Unit) {
                type = Type.FLOAT
                floatValue = value.value
            }

            override fun visitIntValue(value: IntValue, data: Unit) {
                type = Type.INT
                intValue = value.value.toLong()
            }

            override fun visitKClassValue(value: KClassValue, data: Unit) {
                type = Type.CLASS

                when (val classValue = value.value) {
                    is KClassValue.Value.NormalClass -> {
                        classId = stringTable.getQualifiedClassNameIndex(classValue.classId)

                        if (classValue.arrayDimensions > 0) {
                            arrayDimensionCount = classValue.arrayDimensions
                        }
                    }
                    is KClassValue.Value.LocalClass -> {
                        var arrayDimensions = 0
                        var type = classValue.type
                        while (KotlinBuiltIns.isArray(type)) {
                            arrayDimensions++
                            type = type.arguments.single().type
                        }

                        val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor
                            ?: error("Type parameters are not allowed in class literal annotation arguments: $classValue")
                        classId = stringTable.getFqNameIndex(descriptor)

                        if (arrayDimensions > 0) {
                            arrayDimensionCount = arrayDimensions
                        }
                    }
                }
            }

            override fun visitLongValue(value: LongValue, data: Unit) {
                type = Type.LONG
                intValue = value.value
            }

            override fun visitNullValue(value: NullValue, data: Unit) {
                throw UnsupportedOperationException("Null should not appear in annotation arguments")
            }

            override fun visitShortValue(value: ShortValue, data: Unit) {
                type = Type.SHORT
                intValue = value.value.toLong()
            }

            override fun visitStringValue(value: StringValue, data: Unit) {
                type = Type.STRING
                stringValue = stringTable.getStringIndex(value.value)
            }

            override fun visitUByteValue(value: UByteValue, data: Unit?) {
                type = Type.BYTE
                intValue = value.value.toLong()
                flags = Flags.IS_UNSIGNED.toFlags(true)
            }

            override fun visitUShortValue(value: UShortValue, data: Unit?) {
                type = Type.SHORT
                intValue = value.value.toLong()
                flags = Flags.IS_UNSIGNED.toFlags(true)
            }

            override fun visitUIntValue(value: UIntValue, data: Unit?) {
                type = Type.INT
                intValue = value.value.toLong()
                flags = Flags.IS_UNSIGNED.toFlags(true)
            }

            override fun visitULongValue(value: ULongValue, data: Unit?) {
                type = Type.LONG
                intValue = value.value
                flags = Flags.IS_UNSIGNED.toFlags(true)
            }
        }, Unit)
    }

    protected open fun ignoreAnnotation(type: KotlinType): Boolean = false
}
