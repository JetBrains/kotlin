/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.annotationPlatformSupport
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.lexer.KtTokens

object FirInlineClassDeclarationChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val source = declaration.source?.takeIf { it.kind is KtRealSourceElementKind } ?: return

        val hasInlineModifier = declaration.hasModifier(KtTokens.INLINE_KEYWORD)
        val hasValueModifier = declaration.hasModifier(KtTokens.VALUE_KEYWORD)

        if (hasInlineModifier && (LanguageFeature.ExplicitInlineValueClassesSyntax.isDisabled() || !hasValueModifier)) {
            val replacement = when {
                LanguageFeature.ExplicitInlineValueClassesSyntax.isEnabled() -> "'inline value'"
                // With full value classes, a JVM value class without `@JvmInline` isn't inlined.
                LanguageFeature.FullValueClasses.isEnabled() &&
                        context.session.annotationPlatformSupport.jvmInlineAnnotationClassId != null -> "'@JvmInline value'"
                else -> "'value'"
            }
            reporter.reportOn(source, FirErrors.INLINE_CLASS_DEPRECATED, replacement)
        }
    }
}
