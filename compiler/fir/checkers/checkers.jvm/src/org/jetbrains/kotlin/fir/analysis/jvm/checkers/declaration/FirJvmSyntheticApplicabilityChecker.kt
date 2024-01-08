/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_SYNTHETIC_ANNOTATION_CLASS_ID

object FirJvmSyntheticApplicabilityChecker : FirPropertyChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotation = declaration.backingField?.getAnnotationByClassId(JVM_SYNTHETIC_ANNOTATION_CLASS_ID, context.session)
        if (annotation != null && annotation.useSiteTarget == AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD) {
            reporter.reportOn(annotation.source, FirJvmErrors.JVM_SYNTHETIC_ON_DELEGATE, context)
        }
    }
}
