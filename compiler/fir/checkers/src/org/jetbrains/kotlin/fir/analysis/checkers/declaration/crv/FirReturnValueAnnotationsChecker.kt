/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration.crv

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ReturnValueCheckerMode
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

object FirReturnValueAnnotationsChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    private val oldMustUse = ClassId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier("MustUseReturnValue"))

    private fun FirAnnotation.isMustUseReturnValue(session: FirSession): Boolean =
        toAnnotationClassId(session) == StandardClassIds.Annotations.MustUseReturnValues

    private fun FirAnnotation.isOldMustUse(session: FirSession): Boolean = toAnnotationClassId(session) == oldMustUse

    private fun FirAnnotation.isIgnorableValue(session: FirSession): Boolean =
        toAnnotationClassId(session) == StandardClassIds.Annotations.IgnorableReturnValue

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (context.languageVersionSettings.getFlag(AnalysisFlags.returnValueCheckerMode) != ReturnValueCheckerMode.DISABLED) return

        val session = context.session
        declaration.annotations.forEach { annotation ->
            if (annotation.isMustUseReturnValue(session) || annotation.isIgnorableValue(session) || annotation.isOldMustUse(session)) {
                reporter.reportOn(
                    annotation.source,
                    FirErrors.IGNORABILITY_ANNOTATIONS_WITH_CHECKER_DISABLED
                )
            }
        }
    }
}
