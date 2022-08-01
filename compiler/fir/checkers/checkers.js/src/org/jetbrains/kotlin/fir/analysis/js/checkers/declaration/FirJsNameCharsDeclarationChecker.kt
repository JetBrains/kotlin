/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.resolved
import org.jetbrains.kotlin.name.StandardClassIds

private val suggestion = FirJsNameSuggestion()

object FirJsNameCharsDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val hasJsName = declaration.hasAnnotation(StandardClassIds.Annotations.JsName)

        if (
            context.languageVersionSettings.supportsFeature(LanguageFeature.JsAllowInvalidCharsIdentifiersEscaping) ||
            declaration.source?.kind !is KtRealSourceElementKind ||
            declaration is FirPropertyAccessor && !hasJsName ||
            declaration is FirValueParameter
        ) {
            return
        }

        val data = FirDeclarationWithParents(declaration, context.containingDeclarations, context)

        // This case will be reported as WRONG_EXPORTED_DECLARATION for
        // secondary constructor with missing JsName. Skipping it here to simplify further logic.
        if (declaration is FirConstructor && !hasJsName && data.isExportedObject()) {
            return
        }

        val suggestedName = suggestion.suggest(data) ?: return
        if (suggestedName.stable && suggestedName.names.any { sanitizeName(it) != it }) {
            reporter.reportOn(declaration.source, FirJsErrors.NAME_CONTAINS_ILLEGAL_CHARS, context)
        }
    }
}

object FirJsNameCharsExpressionChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        if (
            context.languageVersionSettings.supportsFeature(LanguageFeature.JsAllowInvalidCharsIdentifiersEscaping) ||
            expression.source?.kind !is KtRealSourceElementKind
        ) {
            return
        }

        val name = expression.calleeReference.resolved?.name?.asString() ?: return

        if (sanitizeName(name) != name) {
            reporter.reportOn(expression.calleeReference.source, FirJsErrors.NAME_CONTAINS_ILLEGAL_CHARS, context)
        }
    }
}
