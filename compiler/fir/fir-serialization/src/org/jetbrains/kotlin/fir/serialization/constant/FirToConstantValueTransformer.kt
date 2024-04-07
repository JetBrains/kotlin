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
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.toAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.expressions.FirExpressionEvaluator
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirArrayOfCallTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirArrayOfCallTransformer.Companion.isArrayOfCall
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

@OptIn(PrivateForInline::class)
internal inline fun <reified T : ConstantValue<*>> FirExpression.toConstantValue(
    session: FirSession,
    scopeSession: ScopeSession,
    constValueProvider: ConstValueProvider?
): T? {
    return when (this) {
        // IR evaluator doesn't convert annotation calls to constant values, so we should immediately call the transformer
        is FirAnnotation -> accept(
            FirToConstantValueTransformer,
            FirToConstantValueTransformerData(session, scopeSession, constValueProvider)
        )
        else -> constValueProvider?.findConstantValueFor(this)
            ?: accept(
                FirToConstantValueTransformer,
                FirToConstantValueTransformerData(session, scopeSession, constValueProvider)
            )
    } as? T
}

internal fun FirExpression?.hasConstantValue(session: FirSession): Boolean {
    return this?.accept(FirToConstantValueChecker, session) == true
}

// --------------------------------------------- private implementation part ---------------------------------------------

@PrivateForInline
internal data class FirToConstantValueTransformerData(
    val session: FirSession,
    val scopeSession: ScopeSession,
    val constValueProvider: ConstValueProvider?,
)

private val constantIntrinsicCalls = setOf("toByte", "toLong", "toShort", "toFloat", "toDouble", "toChar", "unaryMinus")

@PrivateForInline
internal object FirToConstantValueTransformer : FirDefaultVisitor<ConstantValue<*>?, FirToConstantValueTransformerData>() {
    private fun FirExpression.toConstantValue(data: FirToConstantValueTransformerData): ConstantValue<*>? {
        return accept(this@FirToConstantValueTransformer, data)
    }

    override fun visitElement(
        element: FirElement,
        data: FirToConstantValueTransformerData
    ): ConstantValue<*>? {
        error("Illegal element as annotation argument: ${element::class.qualifiedName} -> ${element.render()}")
    }

    override fun <T> visitLiteralExpression(
        literalExpression: FirLiteralExpression<T>,
        data: FirToConstantValueTransformerData
    ): ConstantValue<*>? {
        val value = literalExpression.value
        return when (literalExpression.kind) {
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
        val mapping = annotation.convertMapping(data)
        return AnnotationValue.create(annotation.annotationTypeRef.coneType, mapping)
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: FirToConstantValueTransformerData): ConstantValue<*> {
        return visitAnnotation(annotationCall, data)
    }

    private fun FirAnnotation.convertMapping(data: FirToConstantValueTransformerData): Map<Name, ConstantValue<*>> {
        val (session, scopeSession, constValueProvider) = data

        var needsFirEvaluation = false

        val result = if (constValueProvider != null) {
            argumentMapping.mapping.mapValuesTo(mutableMapOf()) { (_, argument) ->
                constValueProvider.findConstantValueFor(argument).also {
                    if (it == null) {
                        needsFirEvaluation = true
                    }
                }
            }
        } else {
            needsFirEvaluation = true
            mutableMapOf()
        }

        if (needsFirEvaluation) {
            val mappingFromFrontend = FirExpressionEvaluator.evaluateAnnotationArguments(this, session)
                ?: errorWithAttachment("Can't compute constant annotation argument mapping") {
                    withFirEntry("annotation", this@convertMapping)
                }
            for (name in argumentMapping.mapping.keys) {
                if (result[name] == null) {
                    mappingFromFrontend[name]?.let {
                        val evaluatedValue = (it as? FirEvaluatorResult.Evaluated)?.result ?: return@let
                        val constantValue = evaluatedValue.accept(this@FirToConstantValueTransformer, data)
                            ?: errorWithAttachment("Cannot convert value for parameter \"$name\" to constant") {
                                withFirEntry("argument", argumentMapping.mapping[name]!!)
                                withFirEntry("annotation", this@convertMapping)
                            }
                        result[name] = constantValue
                    }
                }
            }
        }

        // Fill empty array arguments
        this.resolvedType.scope(
            session,
            scopeSession,
            CallableCopyTypeCalculator.Forced,
            requiredMembersPhase = FirResolvePhase.TYPES
        )?.getDeclaredConstructors()?.firstOrNull()?.let {
            for (parameterSymbol in it.valueParameterSymbols) {
                if (result[parameterSymbol.name] == null && parameterSymbol.resolvedReturnTypeRef.coneType.isArrayType) {
                    result[parameterSymbol.name] = ArrayValue(emptyList())
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        return result as Map<Name, ConstantValue<*>>
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
        return when (val symbol = qualifiedAccessExpression.toResolvedCallableSymbol()) {
            is FirEnumEntrySymbol -> {
                val classId = symbol.callableId.classId ?: return null
                EnumValue(classId, symbol.name)
            }
            is FirConstructorSymbol -> {
                val constructorCall = qualifiedAccessExpression as FirFunctionCall
                val constructedClassSymbol = symbol.containingClassLookupTag()?.toFirRegularClassSymbol(data.session) ?: return null
                if (constructedClassSymbol.classKind != ClassKind.ANNOTATION_CLASS) return null

                val annotationCall = buildAnnotationCall {
                    source = constructorCall.source
                    annotationTypeRef = constructorCall.resolvedType.toFirResolvedTypeRef()
                    typeArguments.addAll(constructorCall.typeArguments)
                    argumentList = constructorCall.argumentList
                    argumentMapping = (argumentList as FirResolvedArgumentList).toAnnotationArgumentMapping()
                    calleeReference = constructorCall.calleeReference
                    containingDeclarationSymbol = FirErrorFunctionSymbol() // anyway it will be unused
                }
                visitAnnotationCall(annotationCall, data)
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

    override fun visitEnumEntryDeserializedAccessExpression(
        enumEntryDeserializedAccessExpression: FirEnumEntryDeserializedAccessExpression,
        data: FirToConstantValueTransformerData,
    ): ConstantValue<*> {
        return EnumValue(
            enumEntryDeserializedAccessExpression.enumClassId,
            enumEntryDeserializedAccessExpression.enumEntryName
        )
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

private object FirToConstantValueChecker : FirDefaultVisitor<Boolean, FirSession>() {
    // `null` value is not treated as a const
    private val supportedConstKinds = setOf<ConstantValueKind<*>>(
        ConstantValueKind.Boolean, ConstantValueKind.Char, ConstantValueKind.String, ConstantValueKind.Float, ConstantValueKind.Double,
        ConstantValueKind.Byte, ConstantValueKind.UnsignedByte, ConstantValueKind.Short, ConstantValueKind.UnsignedShort,
        ConstantValueKind.Int, ConstantValueKind.UnsignedInt, ConstantValueKind.Long, ConstantValueKind.UnsignedLong,
    )

    override fun visitElement(element: FirElement, data: FirSession): Boolean {
        return false
    }

    override fun <T> visitLiteralExpression(
        literalExpression: FirLiteralExpression<T>,
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
