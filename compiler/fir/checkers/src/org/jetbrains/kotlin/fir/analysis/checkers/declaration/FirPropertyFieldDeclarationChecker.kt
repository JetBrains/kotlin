/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.isSubtypeOf

object FirPropertyFieldTypeChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val backingField = declaration.backingField ?: return

        if (!declaration.hasExplicitBackingField) {
            return
        }

        val typeCheckerContext = context.session.typeContext.newBaseTypeCheckerContext(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )

        if (declaration.initializer != null) {
            reporter.reportOn(declaration.initializer?.source, FirErrors.PROPERTY_INITIALIZER_WITH_EXPLICIT_FIELD_DECLARATION, context)
        }

        if (backingField.initializer is FirErrorExpression) {
            reporter.reportOn(backingField.source, FirErrors.PROPERTY_FIELD_DECLARATION_MISSING_INITIALIZER, context)
        }

        if (backingField.isSubtypeOf(declaration, typeCheckerContext)) {
            checkAsPropertyNotSubtype(declaration, context, reporter)
        } else if (declaration.isSubtypeOf(backingField, typeCheckerContext)) {
            checkAsFieldNotSubtype(declaration, context, reporter)
        } else {
            checkAsIndependentTypes(declaration, context, reporter)
        }
    }

    private fun checkAsPropertyNotSubtype(
        property: FirProperty,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (property.isVar && property.setter == null) {
            reporter.reportOn(property.source, FirErrors.PROPERTY_MUST_HAVE_SETTER, context)
        }
    }

    private fun checkAsFieldNotSubtype(
        property: FirProperty,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (property.getter == null) {
            reporter.reportOn(property.source, FirErrors.PROPERTY_MUST_HAVE_GETTER, context)
        }
    }

    private fun checkAsIndependentTypes(
        property: FirProperty,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        checkAsPropertyNotSubtype(property, context, reporter)
        checkAsFieldNotSubtype(property, context, reporter)
    }
}
