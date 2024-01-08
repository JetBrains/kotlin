/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.jvm.FirJvmNamesChecker
import org.jetbrains.kotlin.fir.declarations.*

object FirJvmInvalidAndDangerousCharactersChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = declaration.source
        when (declaration) {
            is FirRegularClass -> FirJvmNamesChecker.checkNameAndReport(declaration.name, source, context, reporter)
            is FirSimpleFunction -> FirJvmNamesChecker.checkNameAndReport(declaration.name, source, context, reporter)
            is FirTypeParameter -> FirJvmNamesChecker.checkNameAndReport(declaration.name, source, context, reporter)
            is FirProperty -> FirJvmNamesChecker.checkNameAndReport(declaration.name, source, context, reporter)
            is FirTypeAlias -> FirJvmNamesChecker.checkNameAndReport(declaration.name, source, context, reporter)
            is FirValueParameter -> FirJvmNamesChecker.checkNameAndReport(declaration.name, source, context, reporter)
            else -> return
        }
    }
}
