/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization.constant

import org.jetbrains.kotlin.constant.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirArrayOfCallTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirArrayOfCallTransformer.Companion.isArrayOfCall
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.types.ConstantValueKind

internal inline fun <reified T : ConstantValue<*>> FirExpression.toConstantValue(
    session: FirSession,
    constValueProvider: ConstValueProvider? = null
): T? {
    return constValueProvider?.findConstantValueFor(this) as? T
        ?: accept(FirToConstantValueTransformer, FirToConstantValueTransformerData(session, constValueProvider)) as? T
}

internal fun FirExpression?.hasConstantValue(session: FirSession): Boolean {
    return this?.accept(FirToConstantValueChecker, session) == true
}

internal data class FirToConstantValueTransformerData(
    val session: FirSession,
    val constValueProvider: ConstValueProvider?,
)

private val constantIntrinsicCalls = setOf("toByte", "toLong", "toShort", "toFloat", "toDouble", "toChar", "unaryMinus")

internal object FirToConstantValueTransformer : FirDefaultVisitor<ConstantValue<*>?, FirToConstantValueTransformerData>() {
    private fun FirExpression.toConstantValue(data: FirToConstantValueTransformerData): ConstantValue<*>? {
        return data.constValueProvider?.findConstantValueFor(this)
            ?: accept(this@FirToConstantValueTransformer, data)
    }

    override fun visitElement(
        element: FirElement,
        data: FirToConstantValueTransformerData
    ): ConstantValue<*>? {
        error("Illegal element as annotation argument: ${element::class.qualifiedName} -> ${element.render()}")
    }

    override fun <T> visitConstExpression(
        constExpression: FirConstExpression<T>,
        data: FirToConstantValueTransformerData
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
            ConstantValueKind.Long -> LongValue((value as Number).toLong())
            ConstantValueKind.UnsignedLong -> ULongValue((value as Number).toLong())
            ConstantValueKind.String -> StringValue(value as String)
            ConstantValueKind.Float -> FloatValue((value as Number).toFloat())
            ConstantValueKind.Double -> DoubleValue((value as Number).toDouble())
            ConstantValueKind.Null -> NullValue
            else -> null
        }
    }

    override fun visitStringConcatenationCall(
        stringConcatenationCall: FirStringConcatenationCall,
        data: FirToConstantValueTransformerData
    ): ConstantValue<*>? {
        val strings = stringConcatenationCall.argumentList.arguments.map { it.toConstantValue(data) }
        if (strings.any { it == null || it !is StringValue }) return null
        return StringValue(strings.joinToString(separator = "") { (it as StringValue).value })
    }

    override fun visitArrayLiteral(
        arrayLiteral: FirArrayLiteral,
        data: FirToConstantValueTransformerData
    ): ConstantValue<*> {
        return ArrayValue(arrayLiteral.argumentList.arguments.mapNotNull { it.toConstantValue(data) })
    }

    override fun visitAnnotation(
        annotation: FirAnnotation,
        data: FirToConstantValueTransformerData,
    ): ConstantValue<*> {
        val mapping = annotation.argumentMapping.mapping.convertToConstantValues(
            data.session,
            data.constValueProvider
        ).addEmptyVarargValuesFor(annotation.toReference()?.toResolvedFunctionSymbol())
        return AnnotationValue.create(annotation.annotationTypeRef.coneType, mapping)
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: FirToConstantValueTransformerData): ConstantValue<*> {
        return visitAnnotation(annotationCall, data)
    }

    override fun visitGetClassCall(
        getClassCall: FirGetClassCall,
        data: FirToConstantValueTransformerData
    ): ConstantValue<*>? {
        return create(getClassCall.argument.resolvedType)
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: FirToConstantValueTransformerData
    ): ConstantValue<*>? {
        val symbol = qualifiedAccessExpression.toResolvedCallableSymbol() ?: return null
        val fir = symbol.fir

        return when {
            symbol.fir is FirEnumEntry -> {
                val classId = symbol.callableId.classId ?: return null
                EnumValue(classId, (symbol.fir as FirEnumEntry).name)
            }

            symbol is FirPropertySymbol -> {
                if (symbol.fir.isConst) symbol.fir.initializer?.toConstantValue(data) else null
            }

            fir is FirField -> {
                if (fir.isFinal) {
                    fir.initializer?.toConstantValue(data)
                } else {
                    null
                }
            }

            symbol is FirConstructorSymbol -> {
                val constructorCall = qualifiedAccessExpression as FirFunctionCall
                val constructedClassSymbol = symbol.containingClassLookupTag()?.toFirRegularClassSymbol(data.session) ?: return null
                if (constructedClassSymbol.classKind != ClassKind.ANNOTATION_CLASS) return null

                val mapping = constructorCall.resolvedArgumentMapping
                    ?.convertToConstantValues(
                        data.session,
                        data.constValueProvider
                    )?.addEmptyVarargValuesFor(symbol)
                    ?: return null
                return AnnotationValue.create(qualifiedAccessExpression.resolvedType, mapping)
            }

            symbol.callableId.packageName.asString() == "kotlin" -> {
                val callableName = symbol.callableId.callableName.asString()
                if (callableName !in constantIntrinsicCalls) return null

                val dispatchReceiver = qualifiedAccessExpression.dispatchReceiver
                val dispatchReceiverValue = dispatchReceiver?.toConstantValue(data) ?: return null
                when (callableName) {
                    "toByte" -> ByteValue((dispatchReceiverValue.value as Number).toByte())
                    "toLong" -> LongValue((dispatchReceiverValue.value as Number).toLong())
                    "toShort" -> ShortValue((dispatchReceiverValue.value as Number).toShort())
                    "toFloat" -> FloatValue((dispatchReceiverValue.value as Number).toFloat())
                    "toDouble" -> DoubleValue((dispatchReceiverValue.value as Number).toDouble())
                    "toChar" -> CharValue((dispatchReceiverValue.value as Number).toInt().toChar())
                    "unaryMinus" -> {
                        when (dispatchReceiverValue) {
                            is ByteValue -> ByteValue((-dispatchReceiverValue.value).toByte())
                            is LongValue -> LongValue(-dispatchReceiverValue.value)
                            is ShortValue -> ShortValue((-dispatchReceiverValue.value).toShort())
                            is FloatValue -> FloatValue(-dispatchReceiverValue.value)
                            is DoubleValue -> DoubleValue(-dispatchReceiverValue.value)
                            else -> null
                        }
                    }
                    else -> null
                }
            }

            else -> null
        }
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: FirToConstantValueTransformerData
    ): ConstantValue<*>? {
        return visitQualifiedAccessExpression(propertyAccessExpression, data)
    }

    override fun visitFunctionCall(
        functionCall: FirFunctionCall,
        data: FirToConstantValueTransformerData
    ): ConstantValue<*>? {
        if (functionCall.isArrayOfCall(data.session)) {
            return FirArrayOfCallTransformer().transformFunctionCall(functionCall, data.session).accept(this, data)
        }
        return visitQualifiedAccessExpression(functionCall, data)
    }

    override fun visitVarargArgumentsExpression(
        varargArgumentsExpression: FirVarargArgumentsExpression,
        data: FirToConstantValueTransformerData,
    ): ConstantValue<*> {
        val arguments = varargArgumentsExpression.arguments.let {
            // Named, spread or array literal arguments for vararg parameters have the form Vararg(Named/Spread?(ArrayLiteral(..))).
            // We need to extract the ArrayLiteral, otherwise we will get two nested ArrayValue as a result.
            (it.singleOrNull()?.unwrapArgument() as? FirArrayLiteral)?.arguments ?: it
        }

        return ArrayValue(arguments.mapNotNull { it.toConstantValue(data) })
    }

    override fun visitWrappedArgumentExpression(
        wrappedArgumentExpression: FirWrappedArgumentExpression,
        data: FirToConstantValueTransformerData,
    ): ConstantValue<*>? {
        return wrappedArgumentExpression.expression.toConstantValue(data)
    }
}

internal object FirToConstantValueChecker : FirDefaultVisitor<Boolean, FirSession>() {
    // `null` value is not treated as a const
    private val supportedConstKinds = setOf<ConstantValueKind<*>>(
        ConstantValueKind.Boolean, ConstantValueKind.Char, ConstantValueKind.String, ConstantValueKind.Float, ConstantValueKind.Double,
        ConstantValueKind.Byte, ConstantValueKind.UnsignedByte, ConstantValueKind.Short, ConstantValueKind.UnsignedShort,
        ConstantValueKind.Int, ConstantValueKind.UnsignedInt, ConstantValueKind.Long, ConstantValueKind.UnsignedLong,
    )

    override fun visitElement(element: FirElement, data: FirSession): Boolean {
        return false
    }

    override fun <T> visitConstExpression(
        constExpression: FirConstExpression<T>,
        data: FirSession
    ): Boolean {
        return constExpression.kind in supportedConstKinds
    }

    override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: FirSession): Boolean {
        return stringConcatenationCall.argumentList.arguments.all { it.accept(this, data) }
    }

    override fun visitArrayLiteral(arrayLiteral: FirArrayLiteral, data: FirSession): Boolean {
        return arrayLiteral.arguments.all { it.accept(this, data) }
    }

    override fun visitAnnotation(annotation: FirAnnotation, data: FirSession): Boolean = true

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: FirSession): Boolean = true

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: FirSession): Boolean {
        return create(getClassCall.argument.resolvedType) != null
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: FirSession): Boolean {
        val symbol = qualifiedAccessExpression.toResolvedCallableSymbol() ?: return false

        return when {
            symbol.fir is FirEnumEntry -> symbol.callableId.classId != null

            symbol is FirPropertySymbol -> symbol.fir.isConst

            symbol is FirFieldSymbol -> symbol.fir.isFinal

            symbol is FirConstructorSymbol -> {
                symbol.containingClassLookupTag()?.toFirRegularClassSymbol(data)?.classKind == ClassKind.ANNOTATION_CLASS
            }

            symbol.callableId.packageName.asString() == "kotlin" -> {
                val dispatchReceiver = qualifiedAccessExpression.dispatchReceiver
                when (symbol.callableId.callableName.asString()) {
                    !in constantIntrinsicCalls -> false
                    else -> dispatchReceiver?.accept(this, data) ?: false
                }
            }

            else -> false
        }
    }

    override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: FirSession): Boolean {
        return visitQualifiedAccessExpression(propertyAccessExpression, data)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: FirSession): Boolean {
        if (functionCall.isArrayOfCall(data)) return functionCall.arguments.all { it.accept(this, data) }
        return visitQualifiedAccessExpression(functionCall, data)
    }

    override fun visitVarargArgumentsExpression(varargArgumentsExpression: FirVarargArgumentsExpression, data: FirSession): Boolean {
        return varargArgumentsExpression.arguments.all { it.accept(this, data) }
    }

    override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: FirSession): Boolean {
        return namedArgumentExpression.expression.accept(this, data)
    }
}
