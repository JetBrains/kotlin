/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isInline

abstract class FirInlinePropertyChecker : FirPropertyChecker() {
    abstract val inlineDeclarationChecker: FirInlineDeclarationChecker

    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.getter?.isInline != true && declaration.setter?.isInline != true) return

        inlineDeclarationChecker.checkCallableDeclaration(declaration, context, reporter)

        if (declaration.hasBackingField || declaration.delegate != null) {
            reporter.reportOn(declaration.source, FirErrors.INLINE_PROPERTY_WITH_BACKING_FIELD, context)
        }
    }
}