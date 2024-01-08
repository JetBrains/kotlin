/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction

object ContractSyntaxV2FunctionChecker : FirSimpleFunctionChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        checkFeatureIsEnabled(declaration, context, reporter)
    }
}

object ContractSyntaxV2PropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        declaration.getter?.let { checkFeatureIsEnabled(it, context, reporter) }
        declaration.setter?.let { checkFeatureIsEnabled(it, context, reporter) }
    }
}

private fun checkFeatureIsEnabled(declaration: FirContractDescriptionOwner, context: CheckerContext, reporter: DiagnosticReporter) {
    val source = declaration.contractDescription.source ?: return
    if (source.elementType != KtNodeTypes.CONTRACT_EFFECT_LIST) return
    val languageVersionSettings = context.languageVersionSettings
    if (!languageVersionSettings.supportsFeature(LanguageFeature.ContractSyntaxV2)) {
        reporter.reportOn(source, FirErrors.UNSUPPORTED_FEATURE, LanguageFeature.ContractSyntaxV2 to languageVersionSettings, context)
    }
}
