/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.isConst

object FirConstPropertyChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isConst) return
        val classKind = (context.containingDeclarations.lastOrNull() as? FirRegularClass)?.classKind
        if (classKind != ClassKind.OBJECT && context.containingDeclarations.size > 1) {
            reporter.reportOn(declaration.source, FirErrors.CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT, context)
        }

        // TODO: Implement checkers for these errors (see ConstModifierChecker in FE1.0):
        // - CONST_VAL_WITH_DELEGATE
        // - CONST_VAL_WITH_GETTER
        // - TYPE_CANT_BE_USED_FOR_CONST_VAL
        // - CONST_VAL_WITHOUT_INITIALIZER
        // - CONST_VAL_WITH_NON_CONST_INITIALIZER
    }
}