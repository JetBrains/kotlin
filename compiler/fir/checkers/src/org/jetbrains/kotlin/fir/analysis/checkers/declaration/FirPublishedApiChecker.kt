/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.name.StandardClassIds

object FirPublishedApiChecker : FirAnnotatedDeclarationChecker() {
    override fun check(declaration: FirAnnotatedDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirMemberDeclaration) return
        if (declaration is FirValueParameter) return
        if (declaration.visibility == Visibilities.Internal) return
        val annotation = declaration.getAnnotationByClassId(StandardClassIds.Annotations.PublishedApi) ?: return
        reporter.reportOn(annotation.source, FirErrors.NON_INTERNAL_PUBLISHED_API, context)
    }
}
