/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.evaluate

import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
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
        firSymbolBuilder: KtSymbolByFirBuilder
    ): List<KtNamedConstantValue> =
        argumentMapping.map { (name, expression) ->
            KtNamedConstantValue(
                name,
                expression.convertConstantExpression(session, firSymbolBuilder) ?: KtUnsupportedConstantValue
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
        firSymbolBuilder: KtSymbolByFirBuilder
    ): Collection<KtConstantValue> =
        mapNotNull { it.convertConstantExpression(session, firSymbolBuilder) }

    private fun Collection<KtConstantValue>.toArrayConstantValueIfNecessary(kotlinOrigin: KtElement?): KtConstantValue {
        return if (size == 1)
            single()
        else
            KtArrayConstantValue(this, kotlinOrigin)
    }

    fun toConstantValue(
        firExpression: FirExpression,
        session: FirSession,
        firSymbolBuilder: KtSymbolByFirBuilder
    ): KtConstantValue? =
        firExpression.convertConstantExpression(session, firSymbolBuilder)

    private fun FirExpression.convertConstantExpression(
        session: FirSession,
        firSymbolBuilder: KtSymbolByFirBuilder
    ): KtConstantValue? {
        return when (this) {
            is FirConstExpression<*> -> convertConstantExpression()
            is FirNamedArgumentExpression -> {
                expression.convertConstantExpression(session, firSymbolBuilder)
            }
            is FirVarargArgumentsExpression -> {
                arguments.convertConstantExpression(session, firSymbolBuilder)
                    .toArrayConstantValueIfNecessary(realPsi as? KtElement)
            }
            is FirArrayOfCall -> {
                argumentList.arguments.convertConstantExpression(session, firSymbolBuilder)
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
                                resolvedSymbol.callableId.className?.asString(),
                                toNamedConstantValue(resultMap, session, firSymbolBuilder),
                                this.realPsi as? KtCallElement
                            )
                        } else null
                    }
                    is FirNamedFunctionSymbol -> {
                        if (resolvedSymbol.callableId.asSingleFqName() in ArrayFqNames.ARRAY_CALL_FQ_NAMES)
                            argumentList.arguments.convertConstantExpression(session, firSymbolBuilder)
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
                        KtEnumEntryConstantValue(
                            resolvedSymbol.fir.buildSymbol(firSymbolBuilder) as KtEnumEntrySymbol,
                            realPsi as? KtElement
                        )
                    }
                    else -> null
                }
            }
            else -> KtUnsupportedConstantValue
        }
    }
}
