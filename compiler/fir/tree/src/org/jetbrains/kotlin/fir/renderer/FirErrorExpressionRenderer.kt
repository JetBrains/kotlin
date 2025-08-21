/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression

abstract class FirErrorExpressionRenderer {
    internal lateinit var components: FirRendererComponents
    protected val printer: FirPrinter get() = components.printer

    fun renderDiagnostic(diagnostic: ConeDiagnostic) {
        printer.print("ERROR_EXPR(${diagnostic.reason})")
    }

    abstract fun renderErrorExpression(errorExpression: FirErrorExpression)
}

class FirErrorExpressionOnlyErrorRenderer : FirErrorExpressionRenderer() {
    override fun renderErrorExpression(errorExpression: FirErrorExpression) {
        renderDiagnostic(errorExpression.diagnostic)
        errorExpression.expression?.let {
            if (errorExpression.source?.kind == KtFakeSourceElementKind.ErrorExpressionForTopLevelLambda) {
                it.accept(components.visitor)
            }
        }
    }
}

class FirErrorExpressionExtendedRenderer : FirErrorExpressionRenderer() {
    override fun renderErrorExpression(errorExpression: FirErrorExpression) {
        renderDiagnostic(errorExpression.diagnostic)
        errorExpression.nonExpressionElement?.accept(components.visitor)
    }
}
