/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.name.StandardClassIds

object FirAllowTypedEqualsApplicabilityChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotation = declaration.annotations.getAnnotationByClassId(StandardClassIds.Annotations.AllowTypedEquals) ?: return
        if (!declaration.isInline) {
            reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_ALLOW_TYPED_EQUALS_ANNOTATION, context)
        }
    }
}