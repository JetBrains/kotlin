/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getAnnotationClassForOptInMarker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.getDeprecationsProviderFromAnnotations
import org.jetbrains.kotlin.fir.declarations.utils.isMethodOfAny
import org.jetbrains.kotlin.fir.declarations.utils.isOverride

object FirAnyDeprecationChecker : FirSimpleFunctionChecker(MppCheckerKind.Common) {
    private val firstKotlin = LanguageVersionSettingsImpl(LanguageVersion.KOTLIN_1_0, ApiVersion.KOTLIN_1_0)

    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isOverride || !declaration.symbol.isMethodOfAny) return
        declaration.annotations.forEach { annotation ->
            val isDeprecationMarker =
                listOf(annotation)
                    .getDeprecationsProviderFromAnnotations(context.session, false)
                    .getDeprecationsInfo(firstKotlin)
                    ?.isNotEmpty() == true
            val isOptInMarker =
                annotation.getAnnotationClassForOptInMarker(context.session) != null
            if (isDeprecationMarker || isOptInMarker)
                reporter.reportOn(annotation.source, FirErrors.POTENTIALLY_NON_REPORTED_ANNOTATION, context)
        }
    }
}
