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
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSubtypeOf

object FirPropertyFieldTypeChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val backingField = declaration.backingField ?: return

        if (!declaration.hasExplicitBackingField) {
            return
        }

        val typeCheckerContext = context.session.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )

        if (declaration.initializer != null) {
            reporter.reportOn(declaration.initializer?.source, FirErrors.PROPERTY_INITIALIZER_WITH_EXPLICIT_FIELD_DECLARATION, context)
        }

        if (backingField.initializer is FirErrorExpression) {
            reporter.reportOn(backingField.source, FirErrors.PROPERTY_FIELD_DECLARATION_MISSING_INITIALIZER, context)
        }

        if (backingField.returnTypeRef.coneType == declaration.returnTypeRef.coneType) {
            reporter.reportOn(backingField.source, FirErrors.REDUNDANT_EXPLICIT_BACKING_FIELD, context)
            return
        }

        if (!backingField.isSubtypeOf(declaration, typeCheckerContext)) {
            checkAsFieldNotSubtype(declaration, context, reporter)
        }

        if (!declaration.isSubtypeOf(backingField, typeCheckerContext)) {
            checkAsPropertyNotSubtype(declaration, context, reporter)
        }
    }

    private val FirPropertyAccessor?.isNotExplicit
        get() = this == null || this is FirDefaultPropertyAccessor

    private fun checkAsPropertyNotSubtype(
        property: FirProperty,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (property.isVar && property.setter.isNotExplicit) {
            reporter.reportOn(property.source, FirErrors.PROPERTY_MUST_HAVE_SETTER, context)
        }
    }

    private fun checkAsFieldNotSubtype(
        property: FirProperty,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (property.getter.isNotExplicit) {
            reporter.reportOn(property.source, FirErrors.PROPERTY_MUST_HAVE_GETTER, context)
        }
    }
}
