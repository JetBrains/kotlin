/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.utils.isCompanionExtension
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.abbreviatedTypeOrSelf
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.util.OperatorNameConventions

object FirCompanionExtensionChecker : FirCallableDeclarationChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirCallableDeclaration) {
        if (!declaration.isCompanionExtension) return

        if (LanguageFeature.CompanionBlocksAndExtensions.isDisabled()) {
            reporter.reportOn(
                declaration.getModifier(KtTokens.COMPANION_KEYWORD)?.source,
                FirErrors.UNSUPPORTED_FEATURE,
                LanguageFeature.CompanionBlocksAndExtensions to context.languageVersionSettings
            )
        }

        if (declaration.isOperator && declaration.symbol.name != OperatorNameConventions.INVOKE) {
            reporter.reportOn(declaration.source, FirErrors.INAPPLICABLE_OPERATOR_MODIFIER, "companion extension")
        }

        declaration.receiverParameter?.let { checkReceiver(it) }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkReceiver(receiverParameter: FirReceiverParameter) {
        receiverParameter.annotations.forEach { reporter.reportOn(it.source, FirErrors.COMPANION_EXTENSION_RECEIVER_ANNOTATED) }

        val typeRef = receiverParameter.typeRef

        typeRef.annotations.forEach { reporter.reportOn(it.source, FirErrors.COMPANION_EXTENSION_RECEIVER_ANNOTATED) }

        val receiverType = typeRef.coneType.abbreviatedTypeOrSelf

        if (receiverType.typeArguments.isNotEmpty()) {
            reporter.reportOn(typeRef.source, FirErrors.COMPANION_EXTENSION_RECEIVER_WITH_TYPE_ARGUMENTS, receiverType)
        }

        val symbol = receiverType.toSymbol() ?: return

        when (symbol) {
            is FirTypeParameterSymbol ->
                reporter.reportOn(typeRef.source, FirErrors.COMPANION_EXTENSION_RECEIVER_IS_TYPE_PARAMETER, receiverType)
            is FirClassLikeSymbol if (symbol.classKind == ClassKind.OBJECT) ->
                reporter.reportOn(typeRef.source, FirErrors.COMPANION_EXTENSION_RECEIVER_IS_OBJECT, receiverType)
            else -> {}
        }
    }
}