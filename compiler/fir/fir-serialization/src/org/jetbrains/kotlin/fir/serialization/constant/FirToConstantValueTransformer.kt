/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization.constant

import org.jetbrains.kotlin.constant.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.toAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirArrayOfCallTransformer.Companion.isArrayOfCall
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal inline fun <reified T : ConstantValue<*>> FirExpression.toConstantValue(
    session: FirSession,
    scopeSession: ScopeSession,
    constValueProvider: ConstValueProvider?
): T? {
    val valueFromIr = constValueProvider?.findConstantValueFor(this)
    if (valueFromIr != null) return valueFromIr as? T

    val valueFromFir = when (this) {
        is FirAnnotation -> this.evaluateToAnnotationValue(session, scopeSession)
        else -> this.toConstantValue(session, scopeSession)
    }
    return valueFromFir as? T
}

internal fun FirExpression?.hasConstantValue(session: FirSession): Boolean {
    return this?.accept(FirToConstantValueChecker, session) == true
}

// --------------------------------------------- private implementation part ---------------------------------------------

private fun FirElement.toConstantValue(session: FirSession, scopeSession: ScopeSession): ConstantValue<*>? {
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
                    val constructedClassSymbol = symbol.containingClassLookupTag()?.toFirRegularClassSymbol(session) ?: return null
                    if (constructedClassSymbol.classKind != ClassKind.ANNOTATION_CLASS) return null

                    val constructorCall = this as FirFunctionCall
                    val mappingToFirExpression = (constructorCall.argumentList as FirResolvedArgumentList).toAnnotationArgumentMapping().mapping
                    val mappingToConstantValues = mappingToFirExpression
                        .mapValues { it.value.toConstantValue(session, scopeSession) ?: return null }
                        .fillEmptyArray(symbol, session)
                    AnnotationValue.create(constructedClassSymbol.classId, mappingToConstantValues)
                }
                else -> null
            }
        }
        is FirAnnotation -> {
            val mappingToFirExpression = this.argumentMapping.mapping
            val mappingToConstantValues = mappingToFirExpression.mapValues { it.value.toConstantValue(session, scopeSession) ?: return null }
            this.toAnnotationValue(mappingToConstantValues, session, scopeSession)
        }
        is FirGetClassCall -> create(this.argument.resolvedType)
        is FirEnumEntryDeserializedAccessExpression -> EnumValue(this.enumClassId, this.enumEntryName)
        is FirArrayLiteral -> ArrayValue(this.argumentList.arguments.mapNotNull { it.toConstantValue(session, scopeSession) })
        is FirVarargArgumentsExpression -> {
            val arguments = this.arguments.let {
                // Named, spread or array literal arguments for vararg parameters have the form Vararg(Named/Spread?(ArrayLiteral(..))).
                // We need to extract the ArrayLiteral, otherwise we will get two nested ArrayValue as a result.
                (it.singleOrNull()?.unwrapArgument() as? FirArrayLiteral)?.arguments ?: it
            }

            return ArrayValue(arguments.mapNotNull { it.toConstantValue(session, scopeSession) })
        }
        else -> null
    }
}

private fun FirAnnotation.evaluateToAnnotationValue(session: FirSession, scopeSession: ScopeSession): AnnotationValue {
    val mappingFromFrontend = FirExpressionEvaluator.evaluateAnnotationArguments(this, session)
        ?: errorWithAttachment("Can't compute constant annotation argument mapping") {
            withFirEntry("annotation", this@evaluateToAnnotationValue)
        }
    val result = argumentMapping.mapping.mapValuesTo(mutableMapOf()) { (name, _) ->
        mappingFromFrontend[name]?.let {
            val evaluatedValue = (it as? FirEvaluatorResult.Evaluated)?.result
            evaluatedValue?.toConstantValue(session, scopeSession)
        } ?: errorWithAttachment("Cannot convert value for parameter \"$name\" to constant") {
            withFirEntry("argument", argumentMapping.mapping[name]!!)
            withFirEntry("annotation", this@evaluateToAnnotationValue)
        }
    }

    return this.toAnnotationValue(result, session, scopeSession)
}

fun FirAnnotation.toAnnotationValue(mapping: Map<Name, ConstantValue<*>>, session: FirSession, scopeSession: ScopeSession): AnnotationValue {
    val coneClassType = this.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.fullyExpandedType(session)
    val classId = coneClassType?.lookupTag?.classId
        ?: errorWithAttachment("Annotation without proper lookup tag }") {
            withFirEntry("annotation", this@toAnnotationValue)
        }

    val constructorSymbol = this@toAnnotationValue
        .resolvedType
        .scope(session, scopeSession, CallableCopyTypeCalculator.Forced, requiredMembersPhase = FirResolvePhase.TYPES)
        ?.getDeclaredConstructors()
        ?.firstOrNull()
    return AnnotationValue.create(classId, mapping.fillEmptyArray(constructorSymbol, session))
}

// For serialization, the compiler should insert an empty array in the place where an array was expected, but wasn't provided
fun Map<Name, ConstantValue<*>>.fillEmptyArray(
    annotationConstructorSymbol: FirConstructorSymbol?,
    session: FirSession
): Map<Name, ConstantValue<*>> {
    if (annotationConstructorSymbol == null) return this
    val additionalEmptyArrays = annotationConstructorSymbol.valueParameterSymbols.mapNotNull { parameterSymbol ->
        if (this[parameterSymbol.name] == null && parameterSymbol.resolvedReturnTypeRef.coneType.fullyExpandedType(session).isArrayType) {
            parameterSymbol.name to ArrayValue(emptyList())
        } else {
            null
        }
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
                when (symbol.callableId.callableName) {
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
