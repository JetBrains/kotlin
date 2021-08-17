/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByFqName
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.name.FqName

object FirSynchronizedAnnotationChecker : FirFunctionChecker() {

    private val SYNCHRONIZED_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Synchronized")

    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotation = declaration.getAnnotationByFqName(SYNCHRONIZED_ANNOTATION_FQ_NAME) ?: return

        if (declaration.isInline) {
            reporter.reportOn(annotation.source, FirJvmErrors.SYNCHRONIZED_ON_INLINE, context)
            return
        }

        val containingClass = declaration.getContainingClassSymbol(context.session) ?: return
        if (containingClass.classKind == ClassKind.INTERFACE) {
            reporter.reportOn(annotation.source, FirJvmErrors.SYNCHRONIZED_IN_INTERFACE, context)
        } else if (declaration.isAbstract) {
            reporter.reportOn(annotation.source, FirJvmErrors.SYNCHRONIZED_ON_ABSTRACT, context)
        }
    }
}