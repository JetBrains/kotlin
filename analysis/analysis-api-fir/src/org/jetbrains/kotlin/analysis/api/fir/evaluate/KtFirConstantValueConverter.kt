/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.evaluate

import org.jetbrains.kotlin.analysis.api.annotations.KtNamedConstantValue
import org.jetbrains.kotlin.analysis.api.symbols.markers.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.ArrayFqNames

internal object KtFirConstantValueConverter {

    fun toNamedConstantValue(
        argumentMapping: Map<String, FirExpression>,
        session: FirSession,
    ): List<KtNamedConstantValue> =
        argumentMapping.map { (name, expression) ->
            KtNamedConstantValue(
                name,
                expression.convertConstantExpression(session) ?: KtUnsupportedConstantValue
            )
        }

    fun <T> toConstantValue(
        firConstExpression: FirConstExpression<T>,
    ): KtLiteralConstantValue<T> =
        firConstExpression.convertConstantExpression()

    private fun <T> FirConstExpression<T>.convertConstantExpression(): KtLiteralConstantValue<T> =
        KtLiteralConstantValue(kind, value, psi as? KtElement)

    private fun Collection<FirExpression>.convertConstantExpression(
        session: FirSession,
    ): Collection<KtConstantValue> =
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

    private fun Collection<KtConstantValue>.toArrayConstantValueIfNecessary(sourcePsi: KtElement?): KtConstantValue? {
        val valueArgument = if (sourcePsi is ValueArgument) sourcePsi else
            (sourcePsi?.parents?.firstOrNull { it is ValueArgument } as? ValueArgument)
        val wrap = valueArgument?.arrayConversionExpected() ?: false
        return if (wrap) {
            KtArrayConstantValue(this, sourcePsi)
        } else {
            singleOrNull()
        }
    }

    fun toConstantValue(
        firExpression: FirExpression,
        session: FirSession,
    ): KtConstantValue? =
        firExpression.convertConstantExpression(session)

    private fun FirExpression.convertConstantExpression(
        session: FirSession,
    ): KtConstantValue? {
        return when (this) {
            is FirConstExpression<*> -> convertConstantExpression()
            is FirNamedArgumentExpression -> {
                expression.convertConstantExpression(session)
            }
            is FirSpreadArgumentExpression -> {
                expression.convertConstantExpression(session)
            }
            is FirVarargArgumentsExpression -> {
                arguments.convertConstantExpression(session).toArrayConstantValueIfNecessary(psi as? KtElement)
            }
            is FirArrayOfCall -> {
                // Desugared collection literals.
                KtArrayConstantValue(argumentList.arguments.convertConstantExpression(session), psi as? KtElement)
            }
            is FirFunctionCall -> {
                val reference = calleeReference as? FirResolvedNamedReference ?: return null
                when (val resolvedSymbol = reference.resolvedSymbol) {
                    is FirConstructorSymbol -> {
                        val classSymbol = resolvedSymbol.getContainingClassSymbol(session) ?: return null
                        if ((classSymbol.fir as? FirClass)?.classKind == ClassKind.ANNOTATION_CLASS) {
                            val resultMap = mutableMapOf<String, FirExpression>()
                            argumentMapping?.entries?.forEach { (arg, param) ->
                                resultMap[param.name.asString()] = arg
                            }
                            KtAnnotationConstantValue(
                                resolvedSymbol.callableId.classId,
                                toNamedConstantValue(resultMap, session),
                                psi as? KtCallElement
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
                        KtEnumEntryConstantValue(resolvedSymbol.callableId, psi as? KtElement)
                    }
                    else -> null
                }
            }
            else -> null
        } ?: FirCompileTimeConstantEvaluator.evaluate(this)?.convertConstantExpression()
    }
}
