/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.FirSyntaxChecker
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.psi.KtParameter

abstract class FirFunctionalTypeParameterSyntaxChecker : FirFunctionTypeRefChecker(), FirSyntaxChecker<FirFunctionTypeParameter, KtParameter> {
    override fun check(typeRef: FirFunctionTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        for (parameter in typeRef.parameters) {
            checkSyntax(parameter, context, reporter)
        }
    }
}
