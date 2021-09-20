/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.evaluate

import org.jetbrains.kotlin.analysis.api.symbols.markers.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtElement
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
        KtLiteralConstantValue(kind, value, realPsi as? KtElement)

    private fun Collection<FirExpression>.convertConstantExpression(
        session: FirSession,
    ): Collection<KtConstantValue> =
        mapNotNull { it.convertConstantExpression(session) }

    private fun Collection<KtConstantValue>.toArrayConstantValueIfNecessary(kotlinOrigin: KtElement?): KtConstantValue {
        return if (size == 1)
            single()
        else
            KtArrayConstantValue(this, kotlinOrigin)
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
            is FirVarargArgumentsExpression -> {
                arguments.convertConstantExpression(session)
                    .toArrayConstantValueIfNecessary(realPsi as? KtElement)
            }
            is FirArrayOfCall -> {
                argumentList.arguments.convertConstantExpression(session)
                    .toArrayConstantValueIfNecessary(realPsi as? KtElement)
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
                                this.realPsi as? KtCallElement
                            )
                        } else null
                    }
                    is FirNamedFunctionSymbol -> {
                        if (resolvedSymbol.callableId.asSingleFqName() in ArrayFqNames.ARRAY_CALL_FQ_NAMES)
                            argumentList.arguments.convertConstantExpression(session)
                                .toArrayConstantValueIfNecessary(realPsi as? KtElement)
                        else null
                    }
                    else -> null
                }
            }
            is FirPropertyAccessExpression -> {
                val reference = calleeReference as? FirResolvedNamedReference ?: return null
                when (val resolvedSymbol = reference.resolvedSymbol) {
                    is FirEnumEntrySymbol -> {
                        KtEnumEntryConstantValue(resolvedSymbol.callableId, realPsi as? KtElement)
                    }
                    else -> null
                }
            }
            else -> KtUnsupportedConstantValue
        }
    }
}
