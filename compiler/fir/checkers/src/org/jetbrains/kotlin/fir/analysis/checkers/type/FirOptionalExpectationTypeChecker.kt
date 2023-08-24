/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isOptionalAnnotationClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

object FirOptionalExpectationTypeChecker : FirTypeRefChecker() {
    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = typeRef.source
        if (source?.kind is KtFakeSourceElementKind) return
        val classSymbol = typeRef.coneTypeSafe<ConeClassLikeType>()?.toRegularClassSymbol(context.session) ?: return
        if (!classSymbol.isOptionalAnnotationClass(context.session)) return

        if (!context.session.moduleData.isCommon) {
            reporter.reportOn(source, FirErrors.OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE, context)
        }

        val annotationContainer = context.annotationContainers.lastOrNull()
        if (annotationContainer?.annotations?.any { it.annotationTypeRef == typeRef } == true) return

        reporter.reportOn(source, FirErrors.OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY, context)
    }
}