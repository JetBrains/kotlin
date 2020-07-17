/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.annotations.AnnotationArgumentVisitor
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.resolve.constants.*

object FirAnnotationArgumentVisitor : AnnotationArgumentVisitor<Unit, FirAnnotationArgumentVisitorData> {
    override fun visitAnnotationValue(value: AnnotationValue, data: FirAnnotationArgumentVisitorData) {
        data.builder.type = ProtoBuf.Annotation.Argument.Value.Type.ANNOTATION
        // TODO: annotation = serializeAnnotation(value.value)
    }

    override fun visitArrayValue(value: ArrayValue, data: FirAnnotationArgumentVisitorData) {
        data.builder.type = ProtoBuf.Annotation.Argument.Value.Type.ARRAY
        for (element in value.value) {
            data.builder.addArrayElement(data.serializer.valueProto(element).build())
        }
    }

    override fun visitBooleanValue(value: BooleanValue, data: FirAnnotationArgumentVisitorData) {
        data.builder.type = ProtoBuf.Annotation.Argument.Value.Type.BOOLEAN
        data.builder.intValue = if (value.value) 1 else 0
    }

    override fun visitByteValue(value: ByteValue, data: FirAnnotationArgumentVisitorData) {
        data.builder.type = ProtoBuf.Annotation.Argument.Value.Type.BYTE
        data.builder.intValue = value.value.toLong()
    }

    override fun visitCharValue(value: CharValue, data: FirAnnotationArgumentVisitorData) {
        data.builder.type = ProtoBuf.Annotation.Argument.Value.Type.CHAR
        data.builder.intValue = value.value.toLong()
    }

    override fun visitDoubleValue(value: DoubleValue, data: FirAnnotationArgumentVisitorData) {
        data.builder.type = ProtoBuf.Annotation.Argument.Value.Type.DOUBLE
        data.builder.doubleValue = value.value
    }

    override fun visitEnumValue(value: EnumValue, data: FirAnnotationArgumentVisitorData) {
        data.builder.type = ProtoBuf.Annotation.Argument.Value.Type.ENUM
        data.builder.classId = data.stringTable.getQualifiedClassNameIndex(value.enumClassId)
        data.builder.enumValueId = data.stringTable.getStringIndex(value.enumEntryName.asString())
    }

    override fun visitErrorValue(value: ErrorValue, data: FirAnnotationArgumentVisitorData) {
        throw UnsupportedOperationException("Error value: $value")
    }

    override fun visitFloatValue(value: FloatValue, data: FirAnnotationArgumentVisitorData) {
        data.builder.type = ProtoBuf.Annotation.Argument.Value.Type.FLOAT
        data.builder.floatValue = value.value
    }

    override fun visitIntValue(value: IntValue, data: FirAnnotationArgumentVisitorData) {
        data.builder.type = ProtoBuf.Annotation.Argument.Value.Type.INT
        data.builder.intValue = value.value.toLong()
    }

    override fun visitKClassValue(value: KClassValue, data: FirAnnotationArgumentVisitorData) {
        data.builder.type = ProtoBuf.Annotation.Argument.Value.Type.CLASS

        when (val classValue = value.value) {
            is KClassValue.Value.NormalClass -> {
                data.builder.classId = data.stringTable.getQualifiedClassNameIndex(classValue.classId)

                if (classValue.arrayDimensions > 0) {
                    data.builder.arrayDimensionCount = classValue.arrayDimensions
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
                    data.builder.arrayDimensionCount = arrayDimensions
                }
            }
        }
    }

    override fun visitLongValue(value: LongValue, data: FirAnnotationArgumentVisitorData) {
        data.builder.type = ProtoBuf.Annotation.Argument.Value.Type.LONG
        data.builder.intValue = value.value
    }

    override fun visitNullValue(value: NullValue, data: FirAnnotationArgumentVisitorData) {
        throw UnsupportedOperationException("Null should not appear in annotation arguments")
    }

    override fun visitShortValue(value: ShortValue, data: FirAnnotationArgumentVisitorData) {
        data.builder.type = ProtoBuf.Annotation.Argument.Value.Type.SHORT
        data.builder.intValue = value.value.toLong()
    }

    override fun visitStringValue(value: StringValue, data: FirAnnotationArgumentVisitorData) {
        data.builder.type = ProtoBuf.Annotation.Argument.Value.Type.STRING
        data.builder.stringValue = data.stringTable.getStringIndex(value.value)
    }

    override fun visitUByteValue(value: UByteValue, data: FirAnnotationArgumentVisitorData) {
        data.builder.type = ProtoBuf.Annotation.Argument.Value.Type.BYTE
        data.builder.intValue = value.value.toLong()
        data.builder.flags = Flags.IS_UNSIGNED.toFlags(true)
    }

    override fun visitUShortValue(value: UShortValue, data: FirAnnotationArgumentVisitorData) {
        data.builder.type = ProtoBuf.Annotation.Argument.Value.Type.SHORT
        data.builder.intValue = value.value.toLong()
        data.builder.flags = Flags.IS_UNSIGNED.toFlags(true)
    }

    override fun visitUIntValue(value: UIntValue, data: FirAnnotationArgumentVisitorData) {
        data.builder.type = ProtoBuf.Annotation.Argument.Value.Type.INT
        data.builder.intValue = value.value.toLong()
        data.builder.flags = Flags.IS_UNSIGNED.toFlags(true)
    }

    override fun visitULongValue(value: ULongValue, data: FirAnnotationArgumentVisitorData) {
        data.builder.type = ProtoBuf.Annotation.Argument.Value.Type.LONG
        data.builder.intValue = value.value
        data.builder.flags = Flags.IS_UNSIGNED.toFlags(true)
    }

}