/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.languageVersionSettings

object FirTypeParametersInObjectChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.classKind == ClassKind.OBJECT && declaration is FirRegularClass) {
            if (declaration.typeParameters.isNotEmpty()) {
                reporter.reportOn(declaration.source, FirErrors.TYPE_PARAMETERS_IN_OBJECT, context)
            }
        } else if (declaration.classKind == ClassKind.CLASS && declaration is FirAnonymousObject) {
            if (declaration.source?.getChild(KtNodeTypes.TYPE_PARAMETER_LIST, depth = 1) != null) {
                val diagnosticFactory =
                    if (context.session.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitTypeParametersInAnonymousObjects)) {
                        FirErrors.TYPE_PARAMETERS_IN_OBJECT
                    } else {
                        FirErrors.TYPE_PARAMETERS_IN_ANONYMOUS_OBJECT
                    }
                reporter.reportOn(declaration.source, diagnosticFactory, context)
            }
        }
    }
}
