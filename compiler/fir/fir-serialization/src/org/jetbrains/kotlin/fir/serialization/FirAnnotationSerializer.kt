/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.annotations.AnnotationArgumentVisitor
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.resolve.constants.*

class FirAnnotationSerializer(private val session: FirSession, private val stringTable: FirElementAwareStringTable) {
    fun serializeAnnotation(annotation: FirAnnotationCall): ProtoBuf.Annotation = ProtoBuf.Annotation.newBuilder().apply {
        val annotationSymbol = annotation.typeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.toSymbol(session)
        val annotationClass = annotationSymbol?.fir ?: error("Annotation type is not a class: ${annotationSymbol?.fir}")

        id = stringTable.getFqNameIndex(annotationClass)

        for (argumentExpression in annotation.argumentList.arguments) {
            if (argumentExpression !is FirNamedArgumentExpression) continue
            val argument = ProtoBuf.Annotation.Argument.newBuilder()
            argument.nameId = stringTable.getStringIndex(argumentExpression.name.asString())
            val constant = argumentExpression.expression as? FirConstExpression<*> ?: continue
            argument.setValue(valueProto(constant.value as? ConstantValue<*> ?: continue))
            addArgument(argument)
        }
    }.build()

    fun valueProto(constant: ConstantValue<*>): ProtoBuf.Annotation.Argument.Value.Builder =
        ProtoBuf.Annotation.Argument.Value.newBuilder().apply {
            constant.accept(object : AnnotationArgumentVisitor<Unit, Unit> {
                override fun visitAnnotationValue(value: AnnotationValue, data: Unit) {
                    type = ProtoBuf.Annotation.Argument.Value.Type.ANNOTATION
                    // TODO: annotation = serializeAnnotation(value.value)
                }

                override fun visitArrayValue(value: ArrayValue, data: Unit) {
                    type = ProtoBuf.Annotation.Argument.Value.Type.ARRAY
                    for (element in value.value) {
                        addArrayElement(valueProto(element).build())
                    }
                }

                override fun visitBooleanValue(value: BooleanValue, data: Unit) {
                    type = ProtoBuf.Annotation.Argument.Value.Type.BOOLEAN
                    intValue = if (value.value) 1 else 0
                }

                override fun visitByteValue(value: ByteValue, data: Unit) {
                    type = ProtoBuf.Annotation.Argument.Value.Type.BYTE
                    intValue = value.value.toLong()
                }

                override fun visitCharValue(value: CharValue, data: Unit) {
                    type = ProtoBuf.Annotation.Argument.Value.Type.CHAR
                    intValue = value.value.toLong()
                }

                override fun visitDoubleValue(value: DoubleValue, data: Unit) {
                    type = ProtoBuf.Annotation.Argument.Value.Type.DOUBLE
                    doubleValue = value.value
                }

                override fun visitEnumValue(value: EnumValue, data: Unit) {
                    type = ProtoBuf.Annotation.Argument.Value.Type.ENUM
                    classId = stringTable.getQualifiedClassNameIndex(value.enumClassId)
                    enumValueId = stringTable.getStringIndex(value.enumEntryName.asString())
                }

                override fun visitErrorValue(value: ErrorValue, data: Unit) {
                    throw UnsupportedOperationException("Error value: $value")
                }

                override fun visitFloatValue(value: FloatValue, data: Unit) {
                    type = ProtoBuf.Annotation.Argument.Value.Type.FLOAT
                    floatValue = value.value
                }

                override fun visitIntValue(value: IntValue, data: Unit) {
                    type = ProtoBuf.Annotation.Argument.Value.Type.INT
                    intValue = value.value.toLong()
                }

                override fun visitKClassValue(value: KClassValue, data: Unit) {
                    type = ProtoBuf.Annotation.Argument.Value.Type.CLASS

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

                            //val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor
                            //    ?: error("Type parameters are not allowed in class literal annotation arguments: $classValue")
                            // TODO: classId = stringTable.getFqNameIndex(descriptor)

                            if (arrayDimensions > 0) {
                                arrayDimensionCount = arrayDimensions
                            }
                        }
                    }
                }

                override fun visitLongValue(value: LongValue, data: Unit) {
                    type = ProtoBuf.Annotation.Argument.Value.Type.LONG
                    intValue = value.value
                }

                override fun visitNullValue(value: NullValue, data: Unit) {
                    throw UnsupportedOperationException("Null should not appear in annotation arguments")
                }

                override fun visitShortValue(value: ShortValue, data: Unit) {
                    type = ProtoBuf.Annotation.Argument.Value.Type.SHORT
                    intValue = value.value.toLong()
                }

                override fun visitStringValue(value: StringValue, data: Unit) {
                    type = ProtoBuf.Annotation.Argument.Value.Type.STRING
                    stringValue = stringTable.getStringIndex(value.value)
                }

                override fun visitUByteValue(value: UByteValue, data: Unit?) {
                    type = ProtoBuf.Annotation.Argument.Value.Type.BYTE
                    intValue = value.value.toLong()
                    flags = Flags.IS_UNSIGNED.toFlags(true)
                }

                override fun visitUShortValue(value: UShortValue, data: Unit?) {
                    type = ProtoBuf.Annotation.Argument.Value.Type.SHORT
                    intValue = value.value.toLong()
                    flags = Flags.IS_UNSIGNED.toFlags(true)
                }

                override fun visitUIntValue(value: UIntValue, data: Unit?) {
                    type = ProtoBuf.Annotation.Argument.Value.Type.INT
                    intValue = value.value.toLong()
                    flags = Flags.IS_UNSIGNED.toFlags(true)
                }

                override fun visitULongValue(value: ULongValue, data: Unit?) {
                    type = ProtoBuf.Annotation.Argument.Value.Type.LONG
                    intValue = value.value
                    flags = Flags.IS_UNSIGNED.toFlags(true)
                }
            }, Unit)
        }
}