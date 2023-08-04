/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.config

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getOwnDeprecation
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.PackageResolutionResult
import org.jetbrains.kotlin.fir.resolve.transformers.resolveToPackageOrClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.checkers.OptInNames
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue

object FirOptInLanguageVersionSettingsChecker : FirLanguageVersionSettingsChecker() {
    override fun check(context: CheckerContext, rawReport: (Boolean, String) -> Unit) {
        context.languageVersionSettings.getFlag(AnalysisFlags.optIn).forEach { fqNameAsString ->
            if (fqNameAsString != OptInNames.REQUIRES_OPT_IN_FQ_NAME.asString()) {
                checkOptInMarkerArgument(context, fqNameAsString, rawReport)
            }
        }
    }

    private fun checkOptInMarkerArgument(context: CheckerContext, fqNameAsString: String, rawReport: (Boolean, String) -> Unit) {
        val packageOrClass = resolveToPackageOrClass(context.session.symbolProvider, FqName(fqNameAsString))
        val symbol = (packageOrClass as? PackageResolutionResult.PackageOrClass)?.classSymbol

        if (symbol == null) {
            rawReport(
                false,
                "Opt-in requirement marker $fqNameAsString is unresolved. Please make sure it's present in the module dependencies"
            )
            return
        }

        if (symbol.getAnnotationByClassId(OptInNames.REQUIRES_OPT_IN_CLASS_ID, context.session) == null) {
            rawReport(false, "Class $fqNameAsString is not an opt-in requirement marker")
            return
        }
        val deprecationInfo = symbol.getOwnDeprecation(context.languageVersionSettings)?.all ?: return
        rawReport(
            deprecationInfo.deprecationLevel != DeprecationLevelValue.WARNING,
            "Opt-in requirement marker $fqNameAsString is deprecated" + deprecationInfo.message?.let { ". $it" }.orEmpty()
        )
    }
}
