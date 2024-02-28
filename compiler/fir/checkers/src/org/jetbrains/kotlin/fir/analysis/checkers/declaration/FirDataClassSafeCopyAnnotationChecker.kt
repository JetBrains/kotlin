/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.resolve.fqName

object FirDataClassSafeCopyAnnotationChecker : FirClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val safeCopy = declaration.annotations.firstOrNull { it.fqName(context.session) == StandardNames.SAFE_COPY_ANNOTATION }
        val unsafeCopy = declaration.annotations.firstOrNull { it.fqName(context.session) == StandardNames.UNSAFE_COPY_ANNOTATION }

        if (safeCopy != null && unsafeCopy != null) {
            reporter.reportOn(unsafeCopy.source, FirErrors.DATA_CLASS_SAFE_COPY_AND_UNSAFE_COPY_ARE_INCOMPATIBLE_ANNOTATIONS, context)
        }

        if (context.languageVersionSettings.supportsFeature(LanguageFeature.DataClassCopyRespectsConstructorVisibility) && safeCopy != null) {
            reporter.reportOn(safeCopy.source, FirErrors.DATA_CLASS_SAFE_COPY_REDUNDANT_ANNOTATION, context)
        }

        if (safeCopy != null && (declaration !is FirRegularClass || !declaration.isData)) {
            reporter.reportOn(safeCopy.source, FirErrors.DATA_CLASS_SAFE_COPY_WRONG_ANNOTATION_TARGET, context)
        }

        if (unsafeCopy != null && (declaration !is FirRegularClass || !declaration.isData)) {
            reporter.reportOn(unsafeCopy.source, FirErrors.DATA_CLASS_SAFE_COPY_WRONG_ANNOTATION_TARGET, context)
        }
    }
}
