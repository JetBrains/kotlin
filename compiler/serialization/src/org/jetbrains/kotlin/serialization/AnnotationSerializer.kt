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

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationArgumentVisitor
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.serialization.ProtoBuf.Annotation.Argument.Value
import org.jetbrains.kotlin.serialization.ProtoBuf.Annotation.Argument.Value.Type
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns

public object AnnotationSerializer {
    public fun serializeAnnotation(annotation: AnnotationDescriptor, stringTable: StringTable): ProtoBuf.Annotation {
        return with(ProtoBuf.Annotation.newBuilder()) {
            val annotationClass = annotation.getType().getConstructor().getDeclarationDescriptor() as? ClassDescriptor
                                  ?: error("Annotation type is not a class: ${annotation.getType()}")

            setId(stringTable.getFqNameIndex(annotationClass))

            for ((parameter, value) in annotation.getAllValueArguments()) {
                val argument = ProtoBuf.Annotation.Argument.newBuilder()
                argument.setNameId(stringTable.getSimpleNameIndex(parameter.getName()))
                argument.setValue(valueProto(value, parameter.getType(), stringTable))
                addArgument(argument)
            }

            build()
        }
    }

    fun valueProto(constant: CompileTimeConstant<*>, type: JetType, nameTable: StringTable): Value.Builder = with(Value.newBuilder()) {
        constant.accept(object : AnnotationArgumentVisitor<Unit, Unit> {
            override fun visitAnnotationValue(value: AnnotationValue, data: Unit) {
                setType(Type.ANNOTATION)
                setAnnotation(serializeAnnotation(value.getValue(), nameTable))
            }

            override fun visitArrayValue(value: ArrayValue, data: Unit) {
                setType(Type.ARRAY)
                for (element in value.getValue()) {
                    addArrayElement(valueProto(element, KotlinBuiltIns.getInstance().getArrayElementType(type), nameTable).build())
                }
            }

            override fun visitBooleanValue(value: BooleanValue, data: Unit) {
                setType(Type.BOOLEAN)
                setIntValue(if (value.getValue()) 1 else 0)
            }

            override fun visitByteValue(value: ByteValue, data: Unit) {
                setType(Type.BYTE)
                setIntValue(value.getValue().toLong())
            }

            override fun visitCharValue(value: CharValue, data: Unit) {
                setType(Type.CHAR)
                setIntValue(value.getValue().toLong())
            }

            override fun visitDoubleValue(value: DoubleValue, data: Unit) {
                setType(Type.DOUBLE)
                setDoubleValue(value.getValue())
            }

            override fun visitEnumValue(value: EnumValue, data: Unit) {
                setType(Type.ENUM)
                val enumEntry = value.getValue()
                setClassId(nameTable.getFqNameIndex(enumEntry.getContainingDeclaration() as ClassDescriptor))
                setEnumValueId(nameTable.getSimpleNameIndex(enumEntry.getName()))
            }

            override fun visitErrorValue(value: ErrorValue, data: Unit) {
                throw UnsupportedOperationException("Error value: $value")
            }

            override fun visitFloatValue(value: FloatValue, data: Unit) {
                setType(Type.FLOAT)
                setFloatValue(value.getValue())
            }

            override fun visitIntValue(value: IntValue, data: Unit) {
                setType(Type.INT)
                setIntValue(value.getValue().toLong())
            }

            override fun visitJavaClassValue(value: JavaClassValue, data: Unit) {
                // TODO: support class literals
                throw UnsupportedOperationException("Class literal annotation arguments are not yet supported: $value")
            }

            override fun visitLongValue(value: LongValue, data: Unit) {
                setType(Type.LONG)
                setIntValue(value.getValue())
            }

            override fun visitNullValue(value: NullValue, data: Unit) {
                throw UnsupportedOperationException("Null should not appear in annotation arguments")
            }

            override fun visitNumberTypeValue(constant: IntegerValueTypeConstant, data: Unit) {
                // TODO: IntegerValueTypeConstant should not occur in annotation arguments
                val number = constant.getValue(type)
                val specificConstant = with(KotlinBuiltIns.getInstance()) {
                    when (type) {
                        getLongType() -> LongValue(number.toLong(), true, true, true)
                        getIntType() -> IntValue(number.toInt(), true, true, true)
                        getShortType() -> ShortValue(number.toShort(), true, true, true)
                        getByteType() -> ByteValue(number.toByte(), true, true, true)
                        else -> throw IllegalStateException("Integer constant $constant has non-integer type $type")
                    }
                }

                specificConstant.accept(this, data)
            }

            override fun visitShortValue(value: ShortValue, data: Unit) {
                setType(Type.SHORT)
                setIntValue(value.getValue().toLong())
            }

            override fun visitStringValue(value: StringValue, data: Unit) {
                setType(Type.STRING)
                setStringValue(nameTable.getStringIndex(value.getValue()))
            }
        }, Unit)

        this
    }
}
