/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.coneType

object FirTypeArgumentsNotAllowedDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirCallableDeclaration<*> && declaration !is FirDefaultPropertyAccessor) {
            val returnTypeRef = declaration.returnTypeRef
            val returnType = returnTypeRef.coneType
            if (returnType is ConeClassErrorType && returnType.reason == "Type arguments not allowed") {
                reporter.report(returnType.source as FirSourceElement)
            }
        }
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let {
            report(FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED.on(it))
        }
    }

}