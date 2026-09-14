/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract

object FirCopyDeclarationChecker : FirCallableDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirCallableDeclaration) {
        if (declaration is FirVariable) {
            if (!declaration.isCopy) return
            if (declaration.isVal && declaration !is FirValueParameter) {
                reporter.reportOn(declaration.source, FirErrors.COPY_VAL)
            }

            if (declaration !is FirProperty) return
            if (declaration.fromPrimaryConstructor != true && !declaration.isAbstract && !declaration.isLocal) {
                reporter.reportOn(declaration.source, FirErrors.COPY_VAR_UNSUPPORTED)
            }
        }

        if (declaration is FirFunction) {
            val copies = (if (declaration.isCopy) 1 else 0) + declaration.valueParameters.count { it.isCopy }
            if (copies > 1) {
                reporter.reportOn(declaration.source, FirErrors.COPY_FUN_TOO_MANY_ARGS)
            }
        }
    }
}
