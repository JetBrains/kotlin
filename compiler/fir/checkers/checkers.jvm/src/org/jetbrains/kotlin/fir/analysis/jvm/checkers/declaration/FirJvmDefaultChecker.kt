/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.config.jvmDefaultMode
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_DEFAULT_WITHOUT_COMPATIBILITY_CLASS_ID
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_DEFAULT_WITH_COMPATIBILITY_CLASS_ID

object FirJvmDefaultChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        val jvmDefaultMode = context.session.languageVersionSettings.jvmDefaultMode
        val session = context.session
        val withoutCompatibility = declaration.getAnnotationByClassId(JVM_DEFAULT_WITHOUT_COMPATIBILITY_CLASS_ID, session)
        if (withoutCompatibility != null && jvmDefaultMode != JvmDefaultMode.ENABLE) {
            reporter.reportOn(withoutCompatibility.source, FirJvmErrors.JVM_DEFAULT_WITHOUT_COMPATIBILITY_NOT_IN_ENABLE_MODE)
        }
        val withCompatibility = declaration.getAnnotationByClassId(JVM_DEFAULT_WITH_COMPATIBILITY_CLASS_ID, session)
        if (withCompatibility != null && jvmDefaultMode != JvmDefaultMode.NO_COMPATIBILITY) {
            reporter.reportOn(withCompatibility.source, FirJvmErrors.JVM_DEFAULT_WITH_COMPATIBILITY_NOT_IN_NO_COMPATIBILITY_MODE)
        }
    }
}
