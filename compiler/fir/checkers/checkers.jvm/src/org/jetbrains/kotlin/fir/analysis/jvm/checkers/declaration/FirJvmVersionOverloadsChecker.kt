/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds

object FirJvmVersionOverloadsChecker : FirFunctionChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        val parameters = declaration.contextParameters + listOfNotNull(declaration.receiverParameter) + declaration.valueParameters
        if (parameters.none { it.hasAnnotation(StandardClassIds.Annotations.IntroducedAt, context.session) }) return

        val jvmOverloadsAnnotation = declaration.getAnnotationByClassId(JvmStandardClassIds.JVM_OVERLOADS_CLASS_ID, context.session)
        if (jvmOverloadsAnnotation != null) {
            reporter.reportOn(
                jvmOverloadsAnnotation.source,
                FirJvmErrors.CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION
            )
        }
    }
}