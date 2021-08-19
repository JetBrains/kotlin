/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByFqName
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.name.FqName

object FirJvmRecordChecker : FirRegularClassChecker() {

    private val JVM_RECORD_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmRecord")

    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotationSource = declaration.getAnnotationByFqName(JVM_RECORD_ANNOTATION_FQ_NAME)?.source ?: return

        val languageVersionSettings = context.session.languageVersionSettings
        if (!languageVersionSettings.supportsFeature(LanguageFeature.JvmRecordSupport)) {
            reporter.reportOn(
                annotationSource,
                FirErrors.UNSUPPORTED_FEATURE,
                LanguageFeature.JvmRecordSupport to languageVersionSettings,
                context
            )
            return
        }

        if (declaration.isLocal) {
            reporter.reportOn(annotationSource, FirJvmErrors.LOCAL_JVM_RECORD, context)
            return
        }

        if (!declaration.isFinal) {
            reporter.reportOn(declaration.source, FirJvmErrors.NON_FINAL_JVM_RECORD, context)
            return
        }

        if (declaration.isEnumClass) {
            reporter.reportOn(declaration.source, FirJvmErrors.ENUM_JVM_RECORD, context)
            return
        }

        if (!declaration.isData) {
            reporter.reportOn(annotationSource, FirJvmErrors.NON_DATA_CLASS_JVM_RECORD, context)
            return
        }

        if (declaration.primaryConstructor?.valueParameters?.isEmpty() == true) {
            reporter.reportOn(annotationSource, FirJvmErrors.JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS, context)
        }
    }
}