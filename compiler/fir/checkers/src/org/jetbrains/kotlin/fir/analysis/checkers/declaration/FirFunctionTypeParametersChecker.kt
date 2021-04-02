/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.isOverride

object FirFunctionTypeParametersChecker : FirFunctionChecker() {

    override fun check(declaration: FirFunction<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        val function = declaration as? FirSimpleFunction ?: return

        if (!function.isOverride) {
            TypeParametersChecker.checkUpperBounds(declaration, context, reporter)
        }
    }
}
