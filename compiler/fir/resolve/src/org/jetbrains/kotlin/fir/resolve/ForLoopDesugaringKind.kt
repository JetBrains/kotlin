/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirAbstractTarget
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirLoopTarget
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirBreakExpression
import org.jetbrains.kotlin.fir.expressions.FirContinueExpression
import org.jetbrains.kotlin.fir.expressions.FirJump
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildBreakExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildContinueExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.visitors.FirTransformer

private object DesugaredForLoopParameter : FirDeclarationDataKey()

private var FirValueParameter.desugaredForLoopParameterData: FirPropertySymbol? by FirDeclarationDataRegistry.data(DesugaredForLoopParameter)

val FirValueParameter.desugaredForLoopParameter: FirPropertySymbol? get() = desugaredForLoopParameterData

val FirValueParameterSymbol.desugaredForLoopParameter: FirPropertySymbol? by FirDeclarationDataRegistry.symbolAccessor(
    key = DesugaredForLoopParameter
)

infix fun FirValueParameter.assignForLoopParameter(loopParameter: FirPropertySymbol) {
    desugaredForLoopParameterData = loopParameter
}

fun FirQualifiedAccessExpression.transformIteratorForLoopParameter(): FirQualifiedAccessExpression =
    transformCalleeReference(object : FirTransformer<Nothing?>() {
        override fun <E : FirElement> transformElement(element: E, data: Nothing?): E = element

        override fun transformResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference, data: Nothing?): FirReference =
            when (val symbol = resolvedNamedReference.resolvedSymbol) {
                is FirValueParameterSymbol -> symbol.desugaredForLoopParameter?.let { loopParameter ->
                    buildResolvedNamedReference {
                        source = resolvedNamedReference.source
                        name = resolvedNamedReference.name
                        resolvedSymbol = loopParameter
                        resolvedSymbolOrigin = resolvedNamedReference.resolvedSymbolOrigin
                    }
                } ?: resolvedNamedReference
                else -> resolvedNamedReference
            }
    }, null)

sealed interface ForLoopDesugaringKind<T : FirAbstractTarget<*>> {

    val target: T

    fun desugarBreakExpression(expression: FirBreakExpression): FirJump<*>

    fun desugarContinueExpression(expression: FirContinueExpression): FirJump<*>

    data class IteratorOperator(override val target: FirLoopTarget) : ForLoopDesugaringKind<FirLoopTarget> {

        override fun desugarBreakExpression(expression: FirBreakExpression): FirJump<*> = buildBreakExpression {
            source = expression.source
            annotations += expression.annotations
            target = this@IteratorOperator.target
        }

        override fun desugarContinueExpression(expression: FirContinueExpression): FirJump<*> = buildContinueExpression {
            source = expression.source
            annotations += expression.annotations
            target = this@IteratorOperator.target
        }
    }

//    data class ForEachCall(override val target: FirFunctionTarget, val isInline: Boolean) : ForLoopDesugaringKind<FirFunctionTarget> {
//
//    }
}

