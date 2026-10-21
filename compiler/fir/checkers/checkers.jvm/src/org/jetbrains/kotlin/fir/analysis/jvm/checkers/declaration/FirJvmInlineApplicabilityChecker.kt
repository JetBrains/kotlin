/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getModifier
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.JvmStandardClassIds

object FirJvmInlineApplicabilityChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    override val platformSpecificCheckerEnabledInMetadataCompilation: Boolean
        get() = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        if (declaration.source?.kind !is KtRealSourceElementKind) return

        val annotation = declaration.getAnnotationByClassId(JvmStandardClassIds.Annotations.JvmInline, context.session)
        val isObject = declaration.classKind == ClassKind.OBJECT

        val valueModifier = declaration.getModifier(KtTokens.VALUE_KEYWORD)
        val hasInlineModifier = declaration.hasModifier(KtTokens.INLINE_KEYWORD)

        if (annotation != null && (valueModifier == null || isObject && fullValueClassesEnabled)) {
            // '@JvmInline' is only applicable to value *classes*, not to non-value declarations (this includes the
            // deprecated inline class syntax) nor to value objects, which are full value classes rather than inline
            // single-field classes. For other wrong targets 'WRONG_MODIFIER_TARGET' is reported instead.
            reporter.reportOn(annotation.source, FirJvmErrors.JVM_INLINE_WITHOUT_VALUE_CLASS)
        } else if (
            annotation == null && valueModifier != null && !declaration.isExpect &&
            !(hasInlineModifier && LanguageFeature.ExplicitInlineValueClassesSyntax.isEnabled())
        ) {
            // do not report anything for non-class declarations, WRONG_MODIFIER will be reported anyway
            if (declaration.classKind != ClassKind.CLASS) return

            if (!fullValueClassesEnabled) {
                // only report if value keyword exists, this ignores the deprecated inline class syntax
                val keyword = valueModifier.source
                val primaryConstructorParameterCount = declaration.primaryConstructorIfAny(context.session)?.valueParameterSymbols?.size
                when (primaryConstructorParameterCount) {
                    // should not advise enabling Full Value Classes or adding @JvmInline, that would NOT help
                    null, 0 -> {}
                    1 -> reporter.reportOn(keyword, FirJvmErrors.VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION)

                    // should advise enabling Full Value Classes, that would help
                    // However, if the parameter number exceeds 1,
                    // [FirValueClassDeclarationChecker] will report the diagnostic in multi-platform way itself.
                }
            }
        }
    }

    context(_: SessionHolder)
    private val fullValueClassesEnabled get() = LanguageFeature.FullValueClasses.isEnabled()
}
