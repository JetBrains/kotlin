/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization.constant

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.types.ConstantValueKind

internal fun FirExpression.toConstantValue(session: FirSession): ConstantValue<*>? = accept(FirToConstantValueTransformer, session)

internal object FirToConstantValueTransformer : FirDefaultVisitor<ConstantValue<*>?, FirSession>() {
    override fun visitElement(
        element: FirElement,
        data: FirSession
    ): ConstantValue<*>? {
        error("Illegal element as annotation argument: ${element::class.qualifiedName} -> ${element.render()}")
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun <T> visitConstExpression(
        constExpression: FirConstExpression<T>,
        data: FirSession
    ): ConstantValue<*>? {
        val value = constExpression.value
        return when (constExpression.kind) {
            ConstantValueKind.Boolean -> BooleanValue(value as Boolean)
            ConstantValueKind.Char -> CharValue(value as Char)
            ConstantValueKind.Byte -> ByteValue((value as Number).toByte())
            ConstantValueKind.UnsignedByte -> UByteValue((value as Number).toByte())
            ConstantValueKind.Short -> ShortValue((value as Number).toShort())
            ConstantValueKind.UnsignedShort -> UShortValue((value as Number).toShort())
            ConstantValueKind.Int -> IntValue((value as Number).toInt())
            ConstantValueKind.UnsignedInt -> UIntValue((value as Number).toInt())
            ConstantValueKind.Long -> LongValue(value as Long)
            ConstantValueKind.UnsignedLong -> ULongValue(value as Long)
            ConstantValueKind.String -> StringValue(value as String)
            ConstantValueKind.Float -> FloatValue(value as Float)
            ConstantValueKind.Double -> DoubleValue(value as Double)
            ConstantValueKind.Null -> NullValue
            else -> null
        }
    }

    override fun visitArrayOfCall(
        arrayOfCall: FirArrayOfCall,
        data: FirSession
    ): ConstantValue<*> {
        return ArrayValue(arrayOfCall.argumentList.arguments.mapNotNull { it.accept(this, data) })
    }

    override fun visitAnnotation(
        annotation: FirAnnotation,
        data: FirSession
    ): ConstantValue<*> {
        return AnnotationValue(annotation)
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: FirSession): ConstantValue<*> {
        return visitAnnotation(annotationCall, data)
    }

    override fun visitGetClassCall(
        getClassCall: FirGetClassCall,
        data: FirSession
    ): ConstantValue<*>? {
        return KClassValue.create(getClassCall.argument.typeRef.coneTypeUnsafe())
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: FirSession
    ): ConstantValue<*>? {
        val symbol = qualifiedAccessExpression.toResolvedCallableSymbol() ?: return null

        return when {
            symbol.fir is FirEnumEntry -> {
                val classId = symbol.fir.returnTypeRef.coneTypeSafe<ConeClassLikeType>()?.classId ?: return null
                EnumValue(classId, (symbol.fir as FirEnumEntry).name)
            }

            symbol is FirConstructorSymbol -> {
                val constructorCall = qualifiedAccessExpression as FirFunctionCall
                val constructedClassSymbol = symbol.containingClass()?.toFirRegularClassSymbol(data) ?: return null
                return if (constructedClassSymbol.classKind == ClassKind.ANNOTATION_CLASS) {
                    AnnotationValue(
                        buildAnnotationCall {
                            argumentMapping = buildAnnotationArgumentMapping {
                                constructorCall.argumentMapping?.forEach { (firExpression, firValueParameter) ->
                                    mapping[firValueParameter.name] = firExpression
                                }
                            }
                            annotationTypeRef = qualifiedAccessExpression.typeRef
                            calleeReference = buildSimpleNamedReference {
                                source = qualifiedAccessExpression.source
                                name = qualifiedAccessExpression.calleeReference.name
                            }
                        }
                    )
                } else {
                    null
                }
            }

            symbol.callableId.packageName.asString() == "kotlin" -> {
                val dispatchReceiver = qualifiedAccessExpression.dispatchReceiver
                val dispatchReceiverValue by lazy { dispatchReceiver.accept(this, data) }
                when (symbol.callableId.callableName.asString()) {
                    "toByte" -> ByteValue((dispatchReceiverValue!!.value as Number).toByte())
                    "toLong" -> LongValue((dispatchReceiverValue!!.value as Number).toLong())
                    "toShort" -> ShortValue((dispatchReceiverValue!!.value as Number).toShort())
                    "toFloat" -> FloatValue((dispatchReceiverValue!!.value as Number).toFloat())
                    "toDouble" -> DoubleValue((dispatchReceiverValue!!.value as Number).toDouble())
                    "toChar" -> CharValue((dispatchReceiverValue!!.value as Number).toChar())
                    "unaryMinus" -> {
                        when (val receiverValue = dispatchReceiverValue) {
                            is ByteValue -> ByteValue((-receiverValue.value).toByte())
                            is LongValue -> LongValue(-receiverValue.value)
                            is ShortValue -> ShortValue((-receiverValue.value).toShort())
                            is FloatValue -> FloatValue(-receiverValue.value)
                            is DoubleValue -> DoubleValue(-receiverValue.value)
                            else -> null
                        }
                    }
                    else -> null
                }
            }

            else -> null
        }
    }

    override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: FirSession): ConstantValue<*>? {
        return visitQualifiedAccessExpression(propertyAccessExpression, data)
    }

    override fun visitFunctionCall(
        functionCall: FirFunctionCall,
        data: FirSession
    ): ConstantValue<*>? {
        return visitQualifiedAccessExpression(functionCall, data)
    }

    override fun visitVarargArgumentsExpression(
        varargArgumentsExpression: FirVarargArgumentsExpression,
        data: FirSession
    ): ConstantValue<*> {
        return ArrayValue(varargArgumentsExpression.arguments.mapNotNull { it.accept(this, data) })
    }

    override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: FirSession): ConstantValue<*>? {
        return namedArgumentExpression.expression.accept(this, data)
    }
}
