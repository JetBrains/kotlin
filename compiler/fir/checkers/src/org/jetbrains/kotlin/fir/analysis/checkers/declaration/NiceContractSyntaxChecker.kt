/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.requireFeatureSupport
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction

object ContractSyntaxV2FunctionChecker : FirSimpleFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirNamedFunction) {
        checkFeatureIsEnabled(declaration)
    }
}

object ContractSyntaxV2PropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        declaration.getter?.let { checkFeatureIsEnabled(it) }
        declaration.setter?.let { checkFeatureIsEnabled(it) }
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkFeatureIsEnabled(
    declaration: FirContractDescriptionOwner,
) {
    val source = declaration.contractDescription?.source ?: return
    if (source.elementType != KtNodeTypes.CONTRACT_EFFECT_LIST) return
    source.requireFeatureSupport(LanguageFeature.ContractSyntaxV2)
}
