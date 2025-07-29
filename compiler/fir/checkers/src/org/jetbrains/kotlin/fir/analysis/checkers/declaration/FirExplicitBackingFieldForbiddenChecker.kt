/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.findClosestClassOrObject
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.declarations.utils.modality

object FirExplicitBackingFieldForbiddenChecker : FirBackingFieldChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirBackingField) {
        if (declaration is FirDefaultPropertyBackingField) {
            return
        }

        if (declaration.propertySymbol.modality != Modality.FINAL) {
            reporter.reportOn(declaration.source, getProperDiagnostic(declaration))
        }

        if (declaration.propertySymbol.isExtension) {
            reporter.reportOn(declaration.source, FirErrors.EXPLICIT_BACKING_FIELD_IN_EXTENSION)
        }
    }

    context(context: CheckerContext)
    private fun getProperDiagnostic(declaration: FirBackingField): KtDiagnosticFactory0 {
        return when {
            context.findClosestClassOrObject()?.classKind == ClassKind.INTERFACE -> FirErrors.EXPLICIT_BACKING_FIELD_IN_INTERFACE
            declaration.propertySymbol.isAbstract -> FirErrors.EXPLICIT_BACKING_FIELD_IN_ABSTRACT_PROPERTY
            else -> FirErrors.NON_FINAL_PROPERTY_WITH_EXPLICIT_BACKING_FIELD
        }
    }
}
