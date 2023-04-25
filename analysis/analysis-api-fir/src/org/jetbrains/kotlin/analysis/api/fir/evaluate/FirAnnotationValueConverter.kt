/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.evaluate

import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.analysis.api.base.KtConstantValueFactory
import org.jetbrains.kotlin.analysis.api.components.KtConstantEvaluationMode
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedTypeQualifierError
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.ArrayFqNames

internal object FirAnnotationValueConverter {
    fun toNamedConstantValue(
        argumentMapping: Map<Name, FirExpression>,
        session: FirSession,
    ): List<KtNamedAnnotationValue> =
        argumentMapping.map { (name, expression) ->
            KtNamedAnnotationValue(
                name,
                expression.convertConstantExpression(session) ?: KtUnsupportedAnnotationValue
            )
        }

    private fun <T> FirConstExpression<T>.convertConstantExpression(): KtConstantAnnotationValue? {
        val expression = psi as? KtElement
        val type = (typeRef as? FirResolvedTypeRef)?.type
        val constantValue = when {
            value == null -> KtConstantValue.KtNullConstantValue(expression)
            type == null -> KtConstantValueFactory.createConstantValue(value, psi as? KtElement)
            type.isBoolean -> KtConstantValue.KtBooleanConstantValue(value as Boolean, expression)
            type.isChar -> KtConstantValue.KtCharConstantValue((value as? Char) ?: (value as Number).toInt().toChar(), expression)
            type.isByte -> KtConstantValue.KtByteConstantValue((value as Number).toByte(), expression)
            type.isUByte -> KtConstantValue.KtUnsignedByteConstantValue((value as Number).toByte().toUByte(), expression)
            type.isShort -> KtConstantValue.KtShortConstantValue((value as Number).toShort(), expression)
            type.isUShort -> KtConstantValue.KtUnsignedShortConstantValue((value as Number).toShort().toUShort(), expression)
            type.isInt -> KtConstantValue.KtIntConstantValue((value as Number).toInt(), expression)
            type.isUInt -> KtConstantValue.KtUnsignedIntConstantValue((value as Number).toInt().toUInt(), expression)
            type.isLong -> KtConstantValue.KtLongConstantValue((value as Number).toLong(), expression)
            type.isULong -> KtConstantValue.KtUnsignedLongConstantValue((value as Number).toLong().toULong(), expression)
            type.isString -> KtConstantValue.KtStringConstantValue(value.toString(), expression)
            type.isFloat -> KtConstantValue.KtFloatConstantValue((value as Number).toFloat(), expression)
            type.isDouble -> KtConstantValue.KtDoubleConstantValue((value as Number).toDouble(), expression)
            else -> null
        }

        return constantValue?.let(::KtConstantAnnotationValue)
    }

    private fun Collection<FirExpression>.convertVarargsExpression(
        session: FirSession,
    ): Pair<Collection<KtAnnotationValue>, KtElement?> {
        var representativePsi: KtElement? = null
        val flattenedVarargs = buildList {
            for (expr in this@convertVarargsExpression) {
                val converted = expr.convertConstantExpression(session) ?: continue

                if (expr is FirSpreadArgumentExpression || expr is FirNamedArgumentExpression) {
                    addAll((converted as KtArrayAnnotationValue).values)
                } else {
                    add(converted)
                }
                representativePsi = representativePsi ?: converted.sourcePsi
            }
        }

        return flattenedVarargs to representativePsi
    }


    fun toConstantValue(
        firExpression: FirExpression,
        session: FirSession,
    ): KtAnnotationValue? = firExpression.convertConstantExpression(session)

    private fun FirExpression.convertConstantExpression(
        session: FirSession,
    ): KtAnnotationValue? {
        val sourcePsi = psi as? KtElement
        return when (this) {
            is FirConstExpression<*> -> convertConstantExpression()
            is FirNamedArgumentExpression -> {
                expression.convertConstantExpression(session)
            }

            is FirSpreadArgumentExpression -> {
                expression.convertConstantExpression(session)
            }

            is FirVarargArgumentsExpression -> {
                // Vararg arguments may have multiple independent expressions associated.
                // Choose one to be the representative PSI value for the entire assembled argument.
                val (annotationValues, representativePsi) = arguments.convertVarargsExpression(session)
                KtArrayAnnotationValue(annotationValues, representativePsi ?: sourcePsi)
            }

            is FirArrayOfCall -> {
                // Desugared collection literals.
                KtArrayAnnotationValue(argumentList.arguments.convertVarargsExpression(session).first, sourcePsi)
            }

            is FirFunctionCall -> {
                val reference = calleeReference as? FirResolvedNamedReference ?: return null
                when (val resolvedSymbol = reference.resolvedSymbol) {
                    is FirConstructorSymbol -> {
                        val classSymbol = resolvedSymbol.getContainingClassSymbol(session) ?: return null
                        if ((classSymbol.fir as? FirClass)?.classKind == ClassKind.ANNOTATION_CLASS) {
                            val resultMap = mutableMapOf<Name, FirExpression>()
                            resolvedArgumentMapping?.entries?.forEach { (arg, param) ->
                                resultMap[param.name] = arg
                            }

                            KtAnnotationApplicationValue(
                                KtAnnotationApplicationWithArgumentsInfo(
                                    resolvedSymbol.callableId.classId,
                                    psi as? KtCallElement,
                                    useSiteTarget = null,
                                    toNamedConstantValue(resultMap, session),
                                    index = null,
                                )
                            )
                        } else null
                    }

                    is FirNamedFunctionSymbol -> {
                        // arrayOf call with a single vararg argument.
                        if (resolvedSymbol.callableId.asSingleFqName() in ArrayFqNames.ARRAY_CALL_FQ_NAMES)
                            argumentList.arguments.single().convertConstantExpression(session)
                        else null
                    }

                    is FirEnumEntrySymbol -> {
                        KtEnumEntryAnnotationValue(resolvedSymbol.callableId, sourcePsi)
                    }

                    else -> null
                }
            }

            is FirPropertyAccessExpression -> {
                val reference = calleeReference as? FirResolvedNamedReference ?: return null
                when (val resolvedSymbol = reference.resolvedSymbol) {
                    is FirEnumEntrySymbol -> {
                        KtEnumEntryAnnotationValue(resolvedSymbol.callableId, sourcePsi)
                    }

                    else -> null
                }
            }

            is FirGetClassCall -> {
                var symbol = (argument as? FirResolvedQualifier)?.symbol
                if (symbol is FirTypeAliasSymbol) {
                    symbol = symbol.fullyExpandedClass(session) ?: symbol
                }
                when {
                    symbol == null -> {
                        val qualifierParts = mutableListOf<String?>()

                        fun process(expression: FirExpression) {
                            val errorType = expression.typeRef.coneType as? ConeErrorType
                            val unresolvedName = when (val diagnostic = errorType?.diagnostic) {
                                is ConeUnresolvedTypeQualifierError -> diagnostic.qualifier
                                is ConeUnresolvedNameError -> diagnostic.qualifier
                                else -> null
                            }
                            qualifierParts += unresolvedName
                            if (errorType != null && expression is FirPropertyAccessExpression) {
                                expression.explicitReceiver?.let { process(it) }
                            }
                        }

                        process(argument)

                        val unresolvedName = qualifierParts.asReversed().filterNotNull().takeIf { it.isNotEmpty() }?.joinToString(".")
                        KtKClassAnnotationValue.KtErrorClassAnnotationValue(sourcePsi, unresolvedName)
                    }
                    symbol.isLocal -> KtKClassAnnotationValue.KtLocalKClassAnnotationValue(
                        symbol.fir.psi as KtClassOrObject,
                        sourcePsi
                    )

                    else -> KtKClassAnnotationValue.KtNonLocalKClassAnnotationValue(symbol.classId, sourcePsi)
                }
            }

            else -> null
        } ?: FirCompileTimeConstantEvaluator.evaluate(this, KtConstantEvaluationMode.CONSTANT_EXPRESSION_EVALUATION)
            ?.convertConstantExpression()
    }
}
