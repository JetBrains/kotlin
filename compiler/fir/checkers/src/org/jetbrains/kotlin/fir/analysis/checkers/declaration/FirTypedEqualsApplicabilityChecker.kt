/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName


object FirTypedEqualsApplicabilityChecker : FirSimpleFunctionChecker() {

    private val TYPED_EQUALS_ANNOTATION_CLASS_ID = ClassId.topLevel(FqName("kotlin.annotations.TypedEquals"))

    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotation = declaration.getAnnotationByClassId(TYPED_EQUALS_ANNOTATION_CLASS_ID) ?: return
        if (!declaration.isTypedEqualsInValueClassBySignature(context.session)) {
            reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_TYPED_EQUALS_ANNOTATION, context)
        }
    }
}