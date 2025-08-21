/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass

object FirTypeParametersInObjectChecker : FirClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (declaration.classKind == ClassKind.OBJECT && declaration is FirRegularClass) {
            if (declaration.typeParameters.isNotEmpty()) {
                reporter.reportOn(declaration.source, FirErrors.TYPE_PARAMETERS_IN_OBJECT)
            }
        } else if (declaration.classKind == ClassKind.CLASS && declaration is FirAnonymousObject) {
            if (declaration.source?.getChild(KtNodeTypes.TYPE_PARAMETER_LIST, depth = 1) != null) {
                reporter.reportOn(declaration.source, FirErrors.TYPE_PARAMETERS_IN_OBJECT)
            }
        }
    }
}
