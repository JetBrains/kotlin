/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.config

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportGlobal
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.PackageResolutionResult
import org.jetbrains.kotlin.fir.resolve.transformers.resolveToPackageOrClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.checkers.OptInNames
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue

object FirOptInLanguageVersionSettingsChecker : FirLanguageVersionSettingsChecker() {
    override fun check(context: CheckerContext, reporter: DiagnosticReporter) {
        context.languageVersionSettings.getFlag(AnalysisFlags.optIn).forEach { fqNameAsString ->
            if (fqNameAsString != OptInNames.REQUIRES_OPT_IN_FQ_NAME.asString()) {
                checkOptInMarkerArgument(context, fqNameAsString, reporter)
            }
        }
    }

    private fun checkOptInMarkerArgument(context: CheckerContext, fqNameAsString: String, reporter: DiagnosticReporter) {
        val fqName = FqName(fqNameAsString)
        val packageOrClass = resolveToPackageOrClass(context.session.symbolProvider, fqName)
        val symbol = (packageOrClass as? PackageResolutionResult.PackageOrClass)?.classSymbol

        if (symbol == null) {
            reporter.reportGlobal(FirErrors.UNRESOLVED_OPT_IN_MARKER, fqName, context)
            return
        }

        if (symbol.getAnnotationByClassId(OptInNames.REQUIRES_OPT_IN_CLASS_ID, context.session) == null) {
            reporter.reportGlobal(FirErrors.OPT_IN_FQNAME_IS_NOT_MARKER, fqName, context)
            return
        }
        val deprecationInfo = symbol.getOwnDeprecation(context.languageVersionSettings)?.all ?: return
        val diagnostic = when (deprecationInfo.deprecationLevel) {
            DeprecationLevelValue.WARNING -> FirErrors.OPT_IN_FQNAME_IS_DEPRECATED
            else -> FirErrors.OPT_IN_FQNAME_IS_DEPRECATED_ERROR
        }
        reporter.reportGlobal(
            diagnostic,
            fqName,
            deprecationInfo.getMessage(context.session).orEmpty(),
            context,
        )
    }
}
