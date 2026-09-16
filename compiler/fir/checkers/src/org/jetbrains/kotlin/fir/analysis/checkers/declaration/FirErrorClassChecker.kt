/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isRichError
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.StandardClassIds

object FirErrorClassChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        if (!declaration.isRichError) return

        if (LanguageFeature.RichErrors.isDisabled()) {
            reporter.reportOn(
                declaration.getModifier(KtTokens.ERROR_KEYWORD)?.source,
                FirErrors.UNSUPPORTED_FEATURE,
                LanguageFeature.RichErrors to context.languageVersionSettings
            )
        }

        for (superTypeRef in declaration.superTypeRefs) {
            if (superTypeRef.coneType.classId != StandardClassIds.RichError && superTypeRef !is FirImplicitBuiltinTypeRef) {
                reporter.reportOn(superTypeRef.source, FirErrors.ERROR_CLASS_HAS_SUPERTYPE)
            }
        }

        for (typeParameterRef in declaration.typeParameters) {
            reporter.reportOn(typeParameterRef.source, FirErrors.ERROR_CLASS_HAS_TYPE_PARAMETER)
        }
    }
}
