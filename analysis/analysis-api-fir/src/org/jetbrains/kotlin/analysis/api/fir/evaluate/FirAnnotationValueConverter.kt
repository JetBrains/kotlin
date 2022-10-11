/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.evaluate

import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.base.KtConstantValueFactory
import org.jetbrains.kotlin.analysis.api.components.KtConstantEvaluationMode
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
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
        val constantValue = KtConstantValueFactory.createConstantValue(value, psi as? KtElement) ?: return null
        return KtConstantAnnotationValue(constantValue)
    }

    private fun Collection<FirExpression>.convertConstantExpression(
        session: FirSession,
    ): Collection<KtAnnotationValue> =
        mapNotNull { it.convertConstantExpression(session) }

    // Refer to KtLightAnnotationParameterList#checkIfToArrayConversionExpected
    private fun ValueArgument?.arrayConversionExpected(): Boolean {
        return when {
            this == null -> false
            this is KtValueArgument && isSpread -> {
                // Anno(*[1,2,3])
                false
            }

            else -> {
                // Anno(a = [1,2,3]) v.s. Anno(1) or Anno(1,2,3)
                !isNamed()
            }
        }
    }

    private fun Collection<KtAnnotationValue>.toArrayConstantValueIfNecessary(sourcePsi: KtElement?): KtAnnotationValue? {
        val valueArgument = if (sourcePsi is ValueArgument) sourcePsi else
            (sourcePsi?.parents?.firstOrNull { it is ValueArgument } as? ValueArgument)
        val wrap = valueArgument?.arrayConversionExpected() ?: false
        return if (wrap) {
            KtArrayAnnotationValue(this, sourcePsi)
        } else {
            singleOrNull()
        }
    }

    fun toConstantValue(
        firExpression: FirExpression,
        session: FirSession,
    ): KtAnnotationValue? =
        firExpression.convertConstantExpression(session)

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
                arguments.convertConstantExpression(session).toArrayConstantValueIfNecessary(sourcePsi)
            }

            is FirArrayOfCall -> {
                // Desugared collection literals.
                KtArrayAnnotationValue(argumentList.arguments.convertConstantExpression(session), sourcePsi)
            }

            is FirFunctionCall -> {
                val reference = calleeReference as? FirResolvedNamedReference ?: return null
                when (val resolvedSymbol = reference.resolvedSymbol) {
                    is FirConstructorSymbol -> {
                        val classSymbol = resolvedSymbol.getContainingClassSymbol(session) ?: return null
                        if ((classSymbol.fir as? FirClass)?.classKind == ClassKind.ANNOTATION_CLASS) {
                            val resultMap = mutableMapOf<Name, FirExpression>()
                            argumentMapping?.entries?.forEach { (arg, param) ->
                                resultMap[param.name] = arg
                            }
                            KtAnnotationApplicationValue(
                                KtAnnotationApplication(
                                    resolvedSymbol.callableId.classId,
                                    psi as? KtCallElement,
                                    useSiteTarget = null,
                                    toNamedConstantValue(resultMap, session),
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
                val symbol = (argument as? FirResolvedQualifier)?.symbol
                when {
                    symbol == null -> KtKClassAnnotationValue.KtErrorClassAnnotationValue(sourcePsi)
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
