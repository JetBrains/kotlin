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
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds

/**
 * All kinds of checks related to inline value classes declaration syntax: old `inline class` syntax, usage of `value class` without
 * `@JvmInline` / `inline` / FVC enabled, inapplicable `@JvmInline`, etc.
 */
object FirInlineValueClassSyntaxChecker : FirRegularClassChecker(Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val source = declaration.source?.takeIf { it.kind is KtRealSourceElementKind } ?: return

        val hasInlineModifier = declaration.hasModifier(KotlinSyntaxTokenIds.INLINE_MODIFIER)
        val hasValueModifier = declaration.hasModifier(KotlinSyntaxTokenIds.VALUE_MODIFIER)
        val annotation = declaration.getAnnotationByClassId(StandardClassIds.Annotations.JvmInline, context.session)

        @OptIn(LegacyJvmInlineHandling::class)
        val platformRequiresJvmInline = context.session.annotationPlatformSupport.hasJvmInlineAnnotation

        if (hasInlineModifier && (LanguageFeature.ExplicitInlineValueClassesSyntax.isDisabled() || !hasValueModifier)) {
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

        when {
            annotation != null && hasValueModifier -> when {
                // This is a case that is not covered by `WRONG_MODIFIER_TARGET` and so we need a separate diagnostic for it:
                //   - `inline value object` is WRONG_MODIFIER_TARGET on `inline`
                //   - `@JvmInline value interface` is wrong modifier target on `value`
                //   - `@JvmInline object`: see below (JVM_INLINE_WITHOUT_VALUE_CLASS)
                //   - `@JvmInline value object` is neither (value object is allowed with FVC)
                declaration.classKind == OBJECT && LanguageFeature.FullValueClasses.isEnabled() && !hasInlineModifier ->
                    reporter.reportOn(annotation.source, FirErrors.JVM_INLINE_ON_VALUE_OBJECT)
                hasInlineModifier && LanguageFeature.ExplicitInlineValueClassesSyntax.isEnabled() ->
                    reporter.reportOn(annotation.source, FirErrors.REDUNDANT_JVM_INLINE)
            }

            annotation != null && !hasValueModifier -> {
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
            }

            // Only an explicit 'value' modifier needs '@JvmInline' / 'inline': deprecated 'inline class' syntax does not.
            annotation == null && hasValueModifier -> {
                // 'inline value class' syntax (since 2.5) already states that the class is inline.
                if (hasInlineModifier && LanguageFeature.ExplicitInlineValueClassesSyntax.isEnabled()) return
                // Value objects are never inline single-field classes.
                if (declaration.classKind != CLASS) return
                // With FVCs enabled, a 'value class' without '@JvmInline' / 'inline' is a full value class, not an inline one.
                if (LanguageFeature.FullValueClasses.isEnabled()) return
                // A wrong number of primary constructor parameters is reported separately, no need to suggest syntax here.
                // TODO: hint to use FVCs if class has > 1 parameters
                if (!declaration.canBeInlineByNumberOfParameters) return

                val diagnostic = when {
                    LanguageFeature.ExplicitInlineValueClassesSyntax.isEnabled() &&
                            (LanguageFeature.ExplicitInlineValueClassesSyntaxRequiredOnAllPlatforms.isEnabled() || platformRequiresJvmInline && !declaration.isExpect) ->
                        FirErrors.INLINE_VALUE_CLASS_WITHOUT_EXPLICIT_INLINE.errorFactory
                    LanguageFeature.ExplicitInlineValueClassesSyntax.isEnabled() ->
                        FirErrors.INLINE_VALUE_CLASS_WITHOUT_EXPLICIT_INLINE.warningFactory
                    platformRequiresJvmInline && !declaration.isExpect ->
                        FirErrors.VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION
                    else -> null // no syntax pre-ExplicitInlineValueClassesSyntax
                }
                diagnostic?.let {
                    reporter.reportOn(source, it)
                }
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
