/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.name.StandardClassIds

object FirSelfTypeChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val self: FirAnnotation? = declaration.getAnnotationByClassId(StandardClassIds.Annotations.Self, context.session)

        if (self != null) {
            val typeParameter = declaration.typeParameters.find { it.symbol.name.asString() == "Self" }
            if (typeParameter != null) {
                reporter.reportOn(typeParameter.source, FirErrors.SELF_TYPE_PARAMETER_FOR_CLASS_WITH_SELF_TYPE, context)
            }
        }
    }

}