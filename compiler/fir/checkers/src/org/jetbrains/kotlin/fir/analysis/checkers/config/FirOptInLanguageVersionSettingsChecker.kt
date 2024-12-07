/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.config

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.PackageResolutionResult
import org.jetbrains.kotlin.fir.resolve.transformers.resolveToPackageOrClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.checkers.OptInNames
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue

object FirOptInLanguageVersionSettingsChecker : FirLanguageVersionSettingsChecker() {
    override fun check(context: CheckerContext, reporter: BaseDiagnosticsCollector.RawReporter) {
        context.languageVersionSettings.getFlag(AnalysisFlags.optIn).forEach { fqNameAsString ->
            if (fqNameAsString != OptInNames.REQUIRES_OPT_IN_FQ_NAME.asString()) {
                checkOptInMarkerArgument(context, fqNameAsString, reporter)
            }
        }
    }

    private fun checkOptInMarkerArgument(context: CheckerContext, fqNameAsString: String, reporter: BaseDiagnosticsCollector.RawReporter) {
        val packageOrClass = resolveToPackageOrClass(context.session.symbolProvider, FqName(fqNameAsString))
        val symbol = (packageOrClass as? PackageResolutionResult.PackageOrClass)?.classSymbol

        if (symbol == null) {
            reporter.reportWarning(
                "Opt-in requirement marker $fqNameAsString is unresolved. Please make sure it's present in the module dependencies"
            )
            return
        }

        if (symbol.getAnnotationByClassId(OptInNames.REQUIRES_OPT_IN_CLASS_ID, context.session) == null) {
            reporter.reportWarning("Class $fqNameAsString is not an opt-in requirement marker")
            return
        }
        val deprecationInfo = symbol.getOwnDeprecation(context.languageVersionSettings)?.all ?: return
        val severity = when (deprecationInfo.deprecationLevel) {
            DeprecationLevelValue.WARNING -> CompilerMessageSeverity.WARNING
            else -> CompilerMessageSeverity.ERROR
        }
        reporter.report(
            "Opt-in requirement marker $fqNameAsString is deprecated" +
                    deprecationInfo.getMessage(context.session)?.let { ". $it" }.orEmpty(),
            severity
        )
    }
}
