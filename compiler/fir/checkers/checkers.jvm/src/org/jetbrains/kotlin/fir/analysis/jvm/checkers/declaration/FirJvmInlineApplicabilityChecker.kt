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
import org.jetbrains.kotlin.fir.isDisabled
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

        val valueModifier = declaration.getModifier(KtTokens.VALUE_KEYWORD)
        val hasInlineModifier = declaration.hasModifier(KtTokens.INLINE_KEYWORD)

        if (
            annotation != null && valueModifier != null && declaration.classKind == ClassKind.OBJECT &&
            LanguageFeature.FullValueClasses.isEnabled()
        ) {
            // Value objects are full value classes rather than inline single-field classes.
            reporter.reportOn(annotation.source, FirJvmErrors.JVM_INLINE_ON_VALUE_OBJECT)
        } else if (annotation != null && valueModifier == null) {
            // '@JvmInline' is only applicable to value *classes*, not to non-value declarations (this includes the
            // deprecated inline class syntax). For other wrong targets 'WRONG_MODIFIER_TARGET' is reported instead.
            reporter.reportOn(annotation.source, FirJvmErrors.JVM_INLINE_WITHOUT_VALUE_CLASS)
        } else if (
            annotation == null &&
            valueModifier != null &&
            !declaration.isExpect &&
            !(hasInlineModifier && LanguageFeature.ExplicitInlineValueClassesSyntax.isEnabled()) &&
            declaration.classKind == ClassKind.CLASS &&
            LanguageFeature.FullValueClasses.isDisabled() &&
            declaration.primaryConstructorIfAny(context.session)?.valueParameterSymbols?.size == 1
        ) {
            // If FVCs are disabled (otherwise, such a class would be considered full) and a class
            //   - has explicit 'value' modifier (because deprecated 'inline class' syntax does not require '@JvmInline' anyway)
            //   - has no '@JvmInline' annotation
            //   - is not 'inline value class' syntax in >= 2.5
            //   - is not expect
            // it is an inline value class on JVM that requires '@JvmInline'.
            // Other conditions are just to filter out cases when we know other diagnostics will be reported.
            // TODO: hint to use FVCs if class has > 1 parameters
            reporter.reportOn(valueModifier.source, FirJvmErrors.VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION)
        }
    }
}
