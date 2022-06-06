/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.utils.visibility

object FirBackingFieldVisibilityConsistencyChecker : FirBackingFieldChecker() {
    override fun check(declaration: FirBackingField, context: CheckerContext, reporter: DiagnosticReporter) {
        // Handled by the `WRONG_MODIFIER_CONTAINING_DECLARATION` diagnostic
        if (declaration.propertySymbol.visibility == Visibilities.Local) {
            return
        }

        val difference = declaration.visibility.compareTo(declaration.propertySymbol.visibility)

        if (difference == null || difference > 0) {
            reporter.reportOn(declaration.source, FirErrors.FIELD_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY, context)
        }
    }
}
