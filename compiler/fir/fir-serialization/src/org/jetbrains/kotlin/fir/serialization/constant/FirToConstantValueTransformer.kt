/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization.constant

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor

fun FirExpression.toConstantValue(): ConstantValue<*>? = accept(FirToConstantValueTransformer, null)

object FirToConstantValueTransformer : FirDefaultVisitor<ConstantValue<*>?, Nothing?>() {
    override fun visitElement(
        element: FirElement,
        data: Nothing?
    ): ConstantValue<*>? {
        error("Illegal element as annotation argument: ${element::class.qualifiedName}")
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun <T> visitConstExpression(
        constExpression: FirConstExpression<T>,
        data: Nothing?
    ): ConstantValue<*>? {
        val value = constExpression.value
        return when (constExpression.kind) {
            FirConstKind.Boolean -> BooleanValue(value as Boolean)
            FirConstKind.Char -> CharValue(value as Char)
            FirConstKind.Byte -> ByteValue(value as Byte)
            FirConstKind.UnsignedByte -> UByteValue(value as Byte)
            FirConstKind.Short -> ShortValue(value as Short)
            FirConstKind.UnsignedShort -> UShortValue(value as Short)
            FirConstKind.Int -> IntValue(value as Int)
            FirConstKind.UnsignedInt -> UIntValue(value as Int)
            FirConstKind.Long -> LongValue(value as Long)
            FirConstKind.UnsignedLong -> ULongValue(value as Long)
            FirConstKind.String -> StringValue(value as String)
            FirConstKind.Float -> FloatValue(value as Float)
            FirConstKind.Double -> DoubleValue(value as Double)
            FirConstKind.Null -> NullValue
            else -> null
        }
    }

    override fun visitArrayOfCall(
        arrayOfCall: FirArrayOfCall,
        data: Nothing?
    ): ConstantValue<*>? {
        return ArrayValue(arrayOfCall.argumentList.arguments.mapNotNull { it.accept(this, null) })
    }

    override fun visitAnnotationCall(
        annotationCall: FirAnnotationCall,
        data: Nothing?
    ): ConstantValue<*>? {
        return AnnotationValue(annotationCall)
    }

    override fun visitGetClassCall(
        getClassCall: FirGetClassCall,
        data: Nothing?
    ): ConstantValue<*>? {
        return KClassValue.create(getClassCall.typeRef.coneTypeUnsafe())
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Nothing?
    ): ConstantValue<*>? {
        val symbol = qualifiedAccessExpression.toResolvedCallableSymbol() ?: return null
        val enumEntry = symbol.fir as? FirEnumEntry ?: return null
        val classId = enumEntry.returnTypeRef.coneType.classId ?: return null
        val outerClassId = classId.outerClassId ?: return null
        return EnumValue(outerClassId, enumEntry.name)
    }
}
