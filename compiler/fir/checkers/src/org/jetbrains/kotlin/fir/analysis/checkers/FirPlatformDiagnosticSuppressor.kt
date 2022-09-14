/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirVariable

interface FirPlatformDiagnosticSuppressor {
    fun shouldReportUnusedParameter(parameter: FirVariable, context: CheckerContext): Boolean

    fun shouldReportNoBody(declaration: FirCallableDeclaration, context: CheckerContext): Boolean

    object Default : FirPlatformDiagnosticSuppressor {
        override fun shouldReportUnusedParameter(parameter: FirVariable, context: CheckerContext): Boolean = true

        override fun shouldReportNoBody(declaration: FirCallableDeclaration, context: CheckerContext): Boolean = true
    }
}
