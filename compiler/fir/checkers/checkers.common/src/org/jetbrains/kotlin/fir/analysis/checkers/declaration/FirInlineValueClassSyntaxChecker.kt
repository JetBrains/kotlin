/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifier
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.LegacyJvmInlineHandling
import org.jetbrains.kotlin.fir.declarations.annotationPlatformSupport
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * All kinds of checks related to inline value classes declaration syntax: old `inline class` syntax, usage of `value class` without
 * `@JvmInline` / `inline` / FVC enabled, inapplicable `@JvmInline`, etc.
 */
object FirInlineValueClassSyntaxChecker : FirRegularClassChecker(Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val source = declaration.source?.takeIf { it.kind is KtRealSourceElementKind } ?: return

        val hasInlineModifier = declaration.hasModifier(KtTokens.INLINE_KEYWORD)
        val valueModifier = declaration.getModifier(KtTokens.VALUE_KEYWORD)
        val annotation = declaration.getAnnotationByClassId(StandardClassIds.Annotations.JvmInline, context.session)

        @OptIn(LegacyJvmInlineHandling::class)
        val platformRequiresJvmInline = context.session.annotationPlatformSupport.hasJvmInlineAnnotation

        if (hasInlineModifier && (LanguageFeature.ExplicitInlineValueClassesSyntax.isDisabled() || valueModifier == null)) {
            val replacement = when {
                LanguageFeature.ExplicitInlineValueClassesSyntax.isEnabled() -> "'inline value'"
                platformRequiresJvmInline && !declaration.isExpect -> "'@JvmInline value'"
                else -> "'value'"
            }
            val diagnostic = if (LanguageFeature.ExplicitInlineValueClassesSyntaxRequiredOnAllPlatforms.isEnabled()) {
                FirErrors.INLINE_CLASS_DEPRECATED_ERROR
            } else {
                FirErrors.INLINE_CLASS_DEPRECATED
            }
            reporter.reportOn(source, diagnostic, replacement)
        }

        if (
            annotation != null && valueModifier != null && declaration.classKind == OBJECT &&
            LanguageFeature.FullValueClasses.isEnabled()
        ) {
            // Value objects are full value classes rather than inline single-field classes.
            reporter.reportOn(annotation.source, FirErrors.JVM_INLINE_ON_VALUE_OBJECT)
        } else if (
            annotation != null &&
            valueModifier != null &&
            hasInlineModifier &&
            LanguageFeature.ExplicitInlineValueClassesSyntax.isEnabled()
        ) {
            reporter.reportOn(annotation.source, FirErrors.REDUNDANT_JVM_INLINE)
        } else if (annotation != null && valueModifier == null) {
            @OptIn(LegacyJvmInlineHandling::class)
            val diagnostic =
                if (
                    context.session.annotationPlatformSupport.hasJvmInlineAnnotation ||
                    LanguageFeature.ExplicitInlineValueClassesSyntaxRequiredOnAllPlatforms.isEnabled()
                ) {
                    FirErrors.JVM_INLINE_WITHOUT_VALUE_CLASS
                } else {
                    FirErrors.JVM_INLINE_WITHOUT_VALUE_CLASS_WARNING
                }
            reporter.reportOn(annotation.source, diagnostic)
        } else if (
            annotation == null &&
            valueModifier != null &&
            !(hasInlineModifier && LanguageFeature.ExplicitInlineValueClassesSyntax.isEnabled()) &&
            declaration.classKind == CLASS &&
            LanguageFeature.FullValueClasses.isDisabled() &&
            declaration.canBeInlineByNumberOfParameters
        ) {
            // If FVCs are disabled (otherwise, such a class would be considered full) and a class
            //   - has explicit 'value' modifier (because deprecated 'inline class' syntax does not require '@JvmInline' anyway)
            //   - has no '@JvmInline' annotation
            //   - is not 'inline value class' syntax in >= 2.5
            // it is an inline class that requires '@JvmInline' / 'inline'.
            // Other conditions are just to filter out cases when we know other diagnostics will be reported.
            // TODO: hint to use FVCs if class has > 1 parameters
            val diagnostic = when {
                LanguageFeature.ExplicitInlineValueClassesSyntax.isEnabled() && !declaration.isExpect &&
                        (LanguageFeature.ExplicitInlineValueClassesSyntaxRequiredOnAllPlatforms.isEnabled() || platformRequiresJvmInline) ->
                    FirErrors.INLINE_VALUE_CLASS_WITHOUT_EXPLICIT_INLINE.errorFactory
                LanguageFeature.ExplicitInlineValueClassesSyntax.isEnabled() ->
                    FirErrors.INLINE_VALUE_CLASS_WITHOUT_EXPLICIT_INLINE.warningFactory
                platformRequiresJvmInline && !declaration.isExpect ->
                    FirErrors.VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION
                else -> null // no syntax pre-ExplicitInlineValueClassesSyntax
            }
            diagnostic?.let {
                reporter.reportOn(valueModifier.source, it)
            }
        }
    }

    context(context: CheckerContext)
    private val FirRegularClass.canBeInlineByNumberOfParameters: Boolean
        get() {
            val constructor = primaryConstructorIfAny(context.session)
            return constructor?.valueParameterSymbols?.size == 1 ||
                    isExpect && LanguageFeature.AllowExpectValueClassesWithNoPrimaryConstructor.isEnabled() && constructor == null
        }
}
