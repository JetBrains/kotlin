/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.psiUtil.belongsToDelegatedSuperTypeEntry

object FirJvmDelegateToDefaultsChecker : FirAnnotationChecker() {
    private val JVM_DELEGATE_TO_DEFAULTS_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmDelegateToDefaults")

    override fun check(expression: FirAnnotation, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.fqName(context.session) == JVM_DELEGATE_TO_DEFAULTS_ANNOTATION_FQ_NAME
            && (expression.source.psi?.belongsToDelegatedSuperTypeEntry() != true)
        ) {
            reporter.reportOn(expression.source, FirErrors.WRONG_ANNOTATION_TARGET, "expression", context)
        }
    }
}