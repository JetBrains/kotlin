/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds.THROWS_ANNOTATION_CLASS_ID

object FirJvmThrowsChecker : FirFunctionChecker(MppCheckerKind.Platform) {
    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val session = context.session
        val annotation = declaration.getAnnotationByClassId(THROWS_ANNOTATION_CLASS_ID, session) ?: return

        val containingClass = declaration.getContainingClassSymbol() ?: return
        when {
            containingClass.classKind == ClassKind.ANNOTATION_CLASS ->
                reporter.reportOn(annotation.source, FirErrors.THROWS_IN_ANNOTATION, context)
        }
    }
}
