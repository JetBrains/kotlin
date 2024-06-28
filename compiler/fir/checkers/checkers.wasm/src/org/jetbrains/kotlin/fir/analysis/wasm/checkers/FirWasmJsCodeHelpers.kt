/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers

import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.name.WebCommonStandardClassIds

fun FirSimpleFunction.hasValidJsCodeBody(): Boolean =
    body?.isValidJsCodeBody() == true

fun FirProperty.hasValidJsCodeBody(): Boolean =
    this.initializer?.isJsCodeCall() == true

private fun FirBlock.isValidJsCodeBody(): Boolean {
    val singleStatement = statements.singleOrNull()
        ?: return false

    return when {
        singleStatement is FirFunctionCall ->
            singleStatement.isJsCodeCall()

        singleStatement is FirReturnExpression && this is FirSingleExpressionBlock ->
            singleStatement.result.isJsCodeCall()

        else ->
            false
    }
}

private fun FirExpression.isJsCodeCall(): Boolean {
    if (this !is FirFunctionCall)
        return false

    val symbol = calleeReference.toResolvedCallableSymbol()
        ?: return false

    return symbol.callableId == WebCommonStandardClassIds.Callables.Js
}
