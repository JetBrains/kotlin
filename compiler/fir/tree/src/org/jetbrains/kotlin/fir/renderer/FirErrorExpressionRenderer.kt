/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.expressions.FirErrorExpression

abstract class FirErrorExpressionRenderer {
    internal lateinit var components: FirRendererComponents
    protected val printer: FirPrinter get() = components.printer

    abstract fun renderErrorExpression(errorExpression: FirErrorExpression)
}

class FirErrorExpressionOnlyErrorRenderer : FirErrorExpressionRenderer() {
    override fun renderErrorExpression(errorExpression: FirErrorExpression) {
        printer.print("ERROR_EXPR(${errorExpression.diagnostic.reason})")
    }
}

class FirErrorExpressionExtendedRenderer : FirErrorExpressionRenderer() {
    override fun renderErrorExpression(errorExpression: FirErrorExpression) {
        printer.print("ERROR_EXPR(${errorExpression.diagnostic.reason})")
        errorExpression.nonExpressionElement?.accept(components.visitor)
    }
}
