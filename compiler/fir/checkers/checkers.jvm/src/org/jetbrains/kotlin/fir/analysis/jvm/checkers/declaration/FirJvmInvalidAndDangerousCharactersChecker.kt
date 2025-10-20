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
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        val source = declaration.source
        when (declaration) {
            is FirRegularClass -> FirJvmNamesChecker.checkNameAndReport(declaration.name, source)
            is FirNamedFunction -> FirJvmNamesChecker.checkNameAndReport(declaration.name, source)
            is FirTypeParameter -> FirJvmNamesChecker.checkNameAndReport(declaration.name, source)
            is FirProperty -> FirJvmNamesChecker.checkNameAndReport(declaration.name, source)
            is FirTypeAlias -> FirJvmNamesChecker.checkNameAndReport(declaration.name, source)
            is FirValueParameter -> FirJvmNamesChecker.checkNameAndReport(declaration.name, source)
            is FirFile -> {
                declaration.packageDirective.packageFqName.pathSegments().forEach {
                    FirJvmNamesChecker.checkNameAndReport(it, declaration.packageDirective.source)
                }
            }
            else -> return
        }
    }
}
