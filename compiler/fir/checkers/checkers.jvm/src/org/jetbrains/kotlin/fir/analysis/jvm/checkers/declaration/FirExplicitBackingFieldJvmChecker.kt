/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBackingFieldChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.name.JvmStandardClassIds

object FirExplicitBackingFieldJvmChecker : FirBackingFieldChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirBackingField) {
        if (declaration is FirDefaultPropertyBackingField) {
            return
        }

        val jvmFieldAnnotation = declaration.getAnnotationByClassId(
            classId = JvmStandardClassIds.Annotations.JvmField,
            session = context.session,
        )

        if (jvmFieldAnnotation != null) {
            reporter.reportOn(jvmFieldAnnotation.source, FirJvmErrors.JVM_FIELD_PROPERTY_WITH_EXPLICIT_BACKING_FIELD)
        }
    }
}