/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.classId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.Repeatable
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.JvmRepeatable
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.Java

object FirJvmRedundantRepeatableChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val kotlinRepeatable = declaration.annotations.find { it.fullyExpandedClassId(context.session) == Repeatable }

        val javaRepeatable = declaration.annotations.find {
            val expandedClassId = it.fullyExpandedClassId(context.session)
            expandedClassId == JvmRepeatable || expandedClassId == Java.Repeatable
        }

        if (kotlinRepeatable != null && javaRepeatable != null) {
            reporter.reportOn(
                kotlinRepeatable.source,
                FirJvmErrors.REDUNDANT_REPEATABLE_ANNOTATION,
                kotlinRepeatable.classId?.asSingleFqName() ?: FqName.ROOT,
                javaRepeatable.classId?.asSingleFqName() ?: FqName.ROOT,
                context
            )
        }
    }
}

