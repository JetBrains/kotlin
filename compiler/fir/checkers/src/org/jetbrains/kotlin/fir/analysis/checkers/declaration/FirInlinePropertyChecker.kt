/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isInline

object FirInlinePropertyChecker : FirPropertyChecker() {

    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.containingClass() == null) return
        if (declaration.effectiveVisibility == EffectiveVisibility.PrivateInClass) return

        if (!declaration.isFinal && (declaration.getter?.isInline == true || declaration.setter?.isInline == true)) {
            reporter.reportOn(declaration.source, FirErrors.DECLARATION_CANT_BE_INLINED, context)
        }

    }
}