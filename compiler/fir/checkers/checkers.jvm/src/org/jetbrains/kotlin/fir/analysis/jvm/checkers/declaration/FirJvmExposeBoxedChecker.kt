/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.resolve.JVM_EXPOSE_BOXED_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.resolve.JVM_INLINE_ANNOTATION_CLASS_ID

object FirJvmExposeBoxedChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val jvmInlineAnnotation = declaration.getAnnotationByClassId(JVM_INLINE_ANNOTATION_CLASS_ID, context.session)
        val jvmExposeBoxedAnnotation = declaration.getAnnotationByClassId(JVM_EXPOSE_BOXED_ANNOTATION_CLASS_ID, context.session)

        if ((jvmInlineAnnotation == null || !declaration.isInline) && jvmExposeBoxedAnnotation != null) {
            reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.JVM_EXPOSE_BOXED_WITHOUT_INLINE, context)
        }
    }
}
