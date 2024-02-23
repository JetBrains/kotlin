/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.name.FqName

private val safeCopyAnnotation = FqName("kotlin.SafeCopy")
private val unsafeCopyAnnotation = FqName("kotlin.UnsafeCopy")

object FirDataClassNonPublicConstructorChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.DataClassCopyRespectsConstructorVisibility)) {
            return
        }
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.WarnAboutDataClassCopyVisibilityChange) &&
            !context.languageVersionSettings.supportsFeature(LanguageFeature.ErrorAboutDataClassCopyVisibilityChange)
        ) {
            return
        }
        if (declaration.classKind != ClassKind.CLASS || !declaration.isData) {
            return
        }
        val isAlreadyAnnotated = declaration.annotations.any {
            val fqName = it.fqName(context.session)
            fqName == safeCopyAnnotation || fqName == unsafeCopyAnnotation
        }
        if (isAlreadyAnnotated) {
            return
        }
        val primaryConstructor = declaration.primaryConstructorIfAny(context.session) ?: return

        if (primaryConstructor.visibility != Visibilities.Public) {
            val factory =
                when (context.languageVersionSettings.supportsFeature(LanguageFeature.ErrorAboutDataClassCopyVisibilityChange)) {
                    true -> FirErrors.DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_ERROR
                    false -> FirErrors.DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING
                }
            reporter.reportOn(primaryConstructor.source, factory, context)
        }
    }
}
