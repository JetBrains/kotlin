/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.hasValOrVar
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny

object FirDataClassPrimaryConstructorChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.classKind != ClassKind.CLASS || !declaration.isData) {
            return
        }

        val primaryConstructor = declaration.primaryConstructorIfAny(context.session)

        if (primaryConstructor == null || primaryConstructor.source.let { it == null || it.kind is KtFakeSourceElementKind }) {
            reporter.reportOn(declaration.source, FirErrors.PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS, context)
            return
        }

        val valueParameters = primaryConstructor.valueParameterSymbols
        if (valueParameters.isEmpty()) {
            reporter.reportOn(primaryConstructor.source, FirErrors.DATA_CLASS_WITHOUT_PARAMETERS, context)
        }
        for (parameter in valueParameters) {
            if (parameter.isVararg) {
                reporter.reportOn(parameter.source, FirErrors.DATA_CLASS_VARARG_PARAMETER, context)
            }
            if (parameter.source?.hasValOrVar() != true) {
                reporter.reportOn(parameter.source, FirErrors.DATA_CLASS_NOT_PROPERTY_PARAMETER, context)
            }
        }
    }
}
