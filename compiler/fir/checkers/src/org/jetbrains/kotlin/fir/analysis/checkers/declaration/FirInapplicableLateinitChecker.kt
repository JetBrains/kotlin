/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isLateInit
import org.jetbrains.kotlin.fir.types.*

object FirInapplicableLateinitChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isLateInit || declaration.returnTypeRef is FirErrorTypeRef) {
            return
        }

        when {
            declaration.isVal ->
                reporter.reportError(declaration.source, "is allowed only on mutable properties", context)

            declaration.initializer != null -> if (declaration.isLocal) {
                reporter.reportError(declaration.source, "is not allowed on local variables with initializer", context)
            } else {
                reporter.reportError(declaration.source, "is not allowed on properties with initializer", context)
            }

            declaration.delegate != null ->
                reporter.reportError(declaration.source, "is not allowed on delegated properties", context)

            declaration.isNullable() ->
                reporter.reportError(declaration.source, "is not allowed on properties of a type with nullable upper bound", context)

            declaration.returnTypeRef.coneType.isPrimitiveOrNullablePrimitive -> if (declaration.isLocal) {
                reporter.reportError(declaration.source, "is not allowed on local variables of primitive types", context)
            } else {
                reporter.reportError(declaration.source, "is not allowed on properties of primitive types", context)
            }

            declaration.hasExplicitBackingField ->
                reporter.reportError(declaration.source, "must be moved to the field declaration", context)

            declaration.hasGetter() || declaration.hasSetter() ->
                reporter.reportError(declaration.source, "is not allowed on properties with a custom getter or setter", context)
        }
    }

    private fun FirProperty.isNullable() = when (val type = returnTypeRef.coneType) {
        is ConeTypeParameterType -> type.isNullable || type.lookupTag.typeParameterSymbol.resolvedBounds.any { it.coneType.isNullable }
        else -> type.isNullable
    }

    private fun FirProperty.hasGetter() = getter != null && getter !is FirDefaultPropertyGetter
    private fun FirProperty.hasSetter() = setter != null && setter !is FirDefaultPropertySetter

    private fun DiagnosticReporter.reportError(source: KtSourceElement?, target: String, context: CheckerContext) {
        reportOn(source, FirErrors.INAPPLICABLE_LATEINIT_MODIFIER, target, context)
    }
}
