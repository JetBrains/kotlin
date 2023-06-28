/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.java.jvmDefaultModeState
import org.jetbrains.kotlin.name.JvmNames.JVM_DEFAULT_NO_COMPATIBILITY_CLASS_ID
import org.jetbrains.kotlin.name.JvmNames.JVM_DEFAULT_WITH_COMPATIBILITY_CLASS_ID

object FirJvmDefaultChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val jvmDefaultMode = context.session.jvmDefaultModeState
        val session = context.session
        val annotationNoCompatibility = declaration.getAnnotationByClassId(JVM_DEFAULT_NO_COMPATIBILITY_CLASS_ID, session)
        if (annotationNoCompatibility != null) {
            val source = annotationNoCompatibility.source
            if (!jvmDefaultMode.isEnabled) {
                reporter.reportOn(source, FirJvmErrors.JVM_DEFAULT_IN_DECLARATION, "JvmDefaultWithoutCompatibility", context)
                return
            }
        }
        val annotationWithCompatibility = declaration.getAnnotationByClassId(JVM_DEFAULT_WITH_COMPATIBILITY_CLASS_ID, session)
        if (annotationWithCompatibility != null) {
            val source = annotationWithCompatibility.source
            when {
                jvmDefaultMode != JvmDefaultMode.ALL_INCOMPATIBLE -> {
                    reporter.reportOn(source, FirJvmErrors.JVM_DEFAULT_WITH_COMPATIBILITY_IN_DECLARATION, context)
                    return
                }
                declaration !is FirRegularClass || !declaration.isInterface -> {
                    reporter.reportOn(source, FirJvmErrors.JVM_DEFAULT_WITH_COMPATIBILITY_NOT_ON_INTERFACE, context)
                    return
                }
            }
        }
    }
}
