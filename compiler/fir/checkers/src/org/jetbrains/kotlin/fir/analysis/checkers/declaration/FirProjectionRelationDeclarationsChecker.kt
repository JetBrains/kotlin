/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkTypeRefForConflictingProjections
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.*

object FirProjectionRelationDeclarationsChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirPropertyAccessor) {
            return
        }

        if (declaration is FirCallableDeclaration) {
            checkTypeRefForConflictingProjections(declaration.returnTypeRef, context, reporter)
        }

        when (declaration) {
            is FirClass -> {
                for (it in declaration.superTypeRefs) {
                    checkTypeRefForConflictingProjections(it, context, reporter)
                }
            }
            is FirTypeAlias ->
                checkTypeRefForConflictingProjections(declaration.expandedTypeRef, context, reporter)
            else -> {}
        }
    }
}
