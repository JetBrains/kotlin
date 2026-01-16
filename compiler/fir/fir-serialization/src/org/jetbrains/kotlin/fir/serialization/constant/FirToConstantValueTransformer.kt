/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization.constant

import org.jetbrains.kotlin.constant.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.getSingleMatchedExpectForActualOrNull
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.toAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.isArrayOfCall
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

context(c: SessionAndScopeSessionHolder)
internal inline fun <reified T : ConstantValue<*>> FirExpression.toConstantValue(): T? {
    val valueFromFir = when (this) {
        is FirAnnotation -> this.evaluateToAnnotationValue()
        else -> this.toConstantValueImpl()
    }
    return (valueFromFir) as? T
}

fun FirExpression?.hasConstantValue(session: FirSession): Boolean {
    return this?.accept(FirToConstantValueChecker, session) == true
}

// --------------------------------------------- private implementation part ---------------------------------------------

context(c: SessionAndScopeSessionHolder)
private fun FirElement.toConstantValueImpl(): ConstantValue<*>? {
    return when (this) {
        is FirLiteralExpression -> {
            val value = this.value
            when (this.kind) {
                ConstantValueKind.Boolean -> BooleanValue(value as Boolean)
                ConstantValueKind.Char -> CharValue(value as Char)
                ConstantValueKind.Byte -> ByteValue((value as Number).toByte())
                ConstantValueKind.UnsignedByte -> UByteValue((value as? Number)?.toByte() ?: (value as UByte).toByte())
                ConstantValueKind.Short -> ShortValue((value as Number).toShort())
                ConstantValueKind.UnsignedShort -> UShortValue((value as? Number)?.toShort() ?: (value as UShort).toShort())
                ConstantValueKind.Int -> IntValue((value as Number).toInt())
                ConstantValueKind.UnsignedInt -> UIntValue((value as? Number)?.toInt() ?: (value as UInt).toInt())
                ConstantValueKind.Long -> LongValue((value as Number).toLong())
                ConstantValueKind.UnsignedLong -> ULongValue((value as? Number)?.toLong() ?: (value as ULong).toLong())
                ConstantValueKind.String -> StringValue(value as String)
                ConstantValueKind.Float -> FloatValue((value as Number).toFloat())
                ConstantValueKind.Double -> DoubleValue((value as Number).toDouble())
                ConstantValueKind.Null -> NullValue
                else -> null
            }
        }
        is FirQualifiedAccessExpression -> {
            when (val symbol = this.toResolvedCallableSymbol()) {
                is FirEnumEntrySymbol -> {
                    val classId = symbol.callableId.classId ?: return null
                    EnumValue(classId, symbol.name)
                }
                is FirConstructorSymbol -> {
                    val constructedClassSymbol = symbol.containingClassLookupTag()?.toRegularClassSymbol() ?: return null
                    if (constructedClassSymbol.classKind != ClassKind.ANNOTATION_CLASS) return null

                    val constructorCall = this as FirFunctionCall
                    val mappingToFirExpression = (constructorCall.argumentList as FirResolvedArgumentList).toAnnotationArgumentMapping().mapping
                    val mappingToConstantValues = mappingToFirExpression
                        .mapValues { it.value.toConstantValueImpl() ?: return null }
                        .fillEmptyArray(symbol, c.session)
                    AnnotationValue.create(constructedClassSymbol.classId, mappingToConstantValues)
                }
                else -> null
            }
        }
        is FirAnnotation -> {
            val mappingToFirExpression = this.argumentMapping.mapping
            val mappingToConstantValues = mappingToFirExpression.mapValues { it.value.toConstantValueImpl() ?: return null }
            this.toAnnotationValue(mappingToConstantValues)
        }
        is FirGetClassCall -> create(this.argument.resolvedType, c.session)
        is FirEnumEntryDeserializedAccessExpression -> EnumValue(this.enumClassId, this.enumEntryName)
        is FirCollectionLiteral -> ArrayValue(this.argumentList.arguments.mapNotNull { it.toConstantValueImpl() })
        is FirVarargArgumentsExpression -> {
            val arguments = this.arguments.let {
                // Named, spread or array literal arguments for vararg parameters have the form Vararg(Named/Spread?(ArrayLiteral(..))).
                // We need to extract the ArrayLiteral, otherwise we will get two nested ArrayValue as a result.
                (it.singleOrNull()?.unwrapArgument() as? FirCollectionLiteral)?.arguments ?: it
            }

            ArrayValue(arguments.mapNotNull { it.toConstantValueImpl() })
        }
        else -> null
    }
}

context(c: SessionAndScopeSessionHolder)
private fun FirAnnotation.evaluateToAnnotationValue(): AnnotationValue {
    val result = buildMap {
        for ((name, value) in argumentMapping.mapping) {
            val constValue = value.toConstantValueImpl() ?: continue
            put(name, constValue)
        }
    }

    return this.toAnnotationValue(result)
}

context(c: SessionAndScopeSessionHolder)
private fun FirAnnotation.toAnnotationValue(mapping: Map<Name, ConstantValue<*>>): AnnotationValue {
    val annotationType = this.annotationTypeRef.coneType.fullyExpandedType()
    val classId = annotationType.classId
        ?: errorWithAttachment("Annotation without proper lookup tag }") {
            withFirEntry("annotation", this@toAnnotationValue)
        }

    val constructorSymbol = this@toAnnotationValue
        .resolvedType
        .scope(CallableCopyTypeCalculator.CalculateDeferredForceLazyResolution, requiredMembersPhase = FirResolvePhase.TYPES)
        ?.getDeclaredConstructors()
        ?.firstOrNull()
    return AnnotationValue.create(classId, mapping.fillEmptyArray(constructorSymbol, c.session))
}

// For serialization, the compiler should insert an empty array in the place where an array was expected, but wasn't provided
fun Map<Name, ConstantValue<*>>.fillEmptyArray(
    annotationConstructorSymbol: FirConstructorSymbol?,
    session: FirSession
): Map<Name, ConstantValue<*>> {
    if (annotationConstructorSymbol == null) return this
    val expectConstructor = annotationConstructorSymbol.getSingleMatchedExpectForActualOrNull()
    val additionalEmptyArrays = annotationConstructorSymbol.valueParameterSymbols.mapNotNull { parameterSymbol ->
        if (this[parameterSymbol.name] != null) return@mapNotNull null
        if (!parameterSymbol.resolvedReturnTypeRef.coneType.fullyExpandedType(session).isArrayType) return@mapNotNull null
        val parameterWithPotentialDefault = expectConstructor?.valueParameterSymbols?.firstOrNull { it.name == parameterSymbol.name } ?: parameterSymbol
        if (parameterWithPotentialDefault.hasDefaultValue) return@mapNotNull null
        parameterSymbol.name to ArrayValue(emptyList())
    }
    return this + additionalEmptyArrays
}

private val constantIntrinsicCalls = OperatorNameConventions.NUMBER_CONVERSIONS + OperatorNameConventions.UNARY_MINUS

private object FirToConstantValueChecker : FirDefaultVisitor<Boolean, FirSession>() {
    // `null` value is not treated as a const
    private val supportedConstKinds = setOf<ConstantValueKind>(
        ConstantValueKind.Boolean, ConstantValueKind.Char, ConstantValueKind.String, ConstantValueKind.Float, ConstantValueKind.Double,
        ConstantValueKind.Byte, ConstantValueKind.UnsignedByte, ConstantValueKind.Short, ConstantValueKind.UnsignedShort,
        ConstantValueKind.Int, ConstantValueKind.UnsignedInt, ConstantValueKind.Long, ConstantValueKind.UnsignedLong,
    )

    override fun visitElement(element: FirElement, data: FirSession): Boolean {
        return false
    }

    override fun visitLiteralExpression(
        literalExpression: FirLiteralExpression,
        data: FirSession
    ): Boolean {
        return literalExpression.kind in supportedConstKinds
    }

    override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: FirSession): Boolean {
        return stringConcatenationCall.argumentList.arguments.all { it.accept(this, data) }
    }

    override fun visitCollectionLiteral(collectionLiteral: FirCollectionLiteral, data: FirSession): Boolean {
        return collectionLiteral.arguments.all { it.accept(this, data) }
    }

    override fun visitAnnotation(annotation: FirAnnotation, data: FirSession): Boolean = true

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: FirSession): Boolean = true

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: FirSession): Boolean {
        return create(getClassCall.argument.resolvedType, data) != null
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: FirSession): Boolean {
        val symbol = qualifiedAccessExpression.toResolvedCallableSymbol() ?: return false

        return when {
            symbol.fir is FirEnumEntry -> symbol.callableId?.classId != null

            symbol is FirPropertySymbol -> symbol.fir.isConst

            symbol is FirFieldSymbol -> symbol.fir.isVal

            symbol is FirConstructorSymbol -> {
                symbol.containingClassLookupTag()?.toRegularClassSymbol(data)?.classKind == ClassKind.ANNOTATION_CLASS
            }

            symbol.callableId?.packageName?.asString() == "kotlin" -> {
                val dispatchReceiver = qualifiedAccessExpression.dispatchReceiver
                when (symbol.name) {
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

    override fun visitExpression(expression: FirExpression, data: FirSession): Boolean {
        // FirExpressionStub could replace constant initializers in fir2ir
        return expression is FirExpressionStub
    }
}
