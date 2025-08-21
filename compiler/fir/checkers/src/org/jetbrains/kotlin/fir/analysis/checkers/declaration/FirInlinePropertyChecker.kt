/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isInline

object FirInlinePropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        if (declaration.getter?.isInline != true && declaration.setter?.isInline != true) return

        FirInlineDeclarationChecker.checkCallableDeclaration(declaration)

        if (declaration.hasBackingField || declaration.delegate != null) {
            when (declaration.source?.kind) {
                KtFakeSourceElementKind.PropertyFromParameter -> reporter.reportOn(
                    declaration.source, FirErrors.INLINE_PROPERTY_WITH_BACKING_FIELD_DEPRECATION
                )
                else -> reporter.reportOn(
                    declaration.source, FirErrors.INLINE_PROPERTY_WITH_BACKING_FIELD
                )
            }
        }
    }
}
