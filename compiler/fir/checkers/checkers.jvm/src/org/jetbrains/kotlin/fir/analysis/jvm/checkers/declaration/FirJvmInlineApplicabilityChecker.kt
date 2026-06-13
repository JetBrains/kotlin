/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.SuspiciousValueClassCheck
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isValue
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.resolve.JVM_INLINE_ANNOTATION_CLASS_ID

object FirJvmInlineApplicabilityChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    override val platformSpecificCheckerEnabledInMetadataCompilation: Boolean
        get() = true

    @OptIn(SuspiciousValueClassCheck::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val annotation = declaration.getAnnotationByClassId(JVM_INLINE_ANNOTATION_CLASS_ID, context.session)
        if (annotation != null && !declaration.isValue) {
            // only report if value keyword does not exist, this includes the deprecated inline class syntax
            reporter.reportOn(annotation.source, FirJvmErrors.JVM_INLINE_WITHOUT_VALUE_CLASS)
        } else if (annotation == null && declaration.isValue && !declaration.isExpect) {
            // do not report anything for non-class declarations, WRONG_MODIFIER will be reported anyway
            if (declaration.classKind != ClassKind.CLASS) return
            val isFullValueClassSupportEnabled = LanguageFeature.FullValueClasses.isEnabled()
            if (!isFullValueClassSupportEnabled) {
                // only report if value keyword exists, this ignores the deprecated inline class syntax
                val keyword = declaration.getModifier(KtTokens.VALUE_KEYWORD)!!.source
                val primaryConstructorParameterCount = declaration.primaryConstructorIfAny(context.session)?.valueParameterSymbols?.size
                val jvmInlineMultiFieldValueClassesEnabled = LanguageFeature.JvmInlineMultiFieldValueClasses.isEnabled()
                when {
                    // should not advise switching to Full Value Classes, that would NOT help
                    primaryConstructorParameterCount == null || primaryConstructorParameterCount == 0 -> {
                        reporter.reportOn(keyword, FirJvmErrors.VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION)
                    }

                    // should advise switching to Full Value Classes, that would help
                    // However, if the parameter number exceeds 1 and jvmInlineMultiFieldValueClassesEnabled is off,
                    // [FirValueClassDeclarationChecker] will report the diagnostic in multi-platform way itself.
                    primaryConstructorParameterCount == 1 || jvmInlineMultiFieldValueClassesEnabled -> {
                        reporter.reportOn(
                            keyword,
                            FirErrors.UNSUPPORTED_FEATURE,
                            LanguageFeature.FullValueClasses to context.languageVersionSettings
                        )
                    }

                    // The complexity appears because in this left case there are both reasons to advise switching to full value classes:
                    // - Missing @JvmInline
                    // - Multiple parameters, having no [LanguageFeature.JvmInlineMultiFieldValueClasses]
                }
            }
        }
    }
}
