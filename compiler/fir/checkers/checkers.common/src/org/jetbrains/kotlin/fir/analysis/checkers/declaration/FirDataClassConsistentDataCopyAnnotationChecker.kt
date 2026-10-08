/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isExportedToJs
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.name.StandardClassIds

object FirDataClassConsistentDataCopyAnnotationChecker : FirClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        val consistentCopy = declaration.getAnnotationByClassId(StandardClassIds.Annotations.ConsistentCopyVisibility, context.session)
        val exposedCopy = declaration.getAnnotationByClassId(StandardClassIds.Annotations.ExposedCopyVisibility, context.session)

        when {
            consistentCopy != null && (declaration !is FirRegularClass || !declaration.isData) -> {
                reporter.reportOn(consistentCopy.source, FirErrors.DATA_CLASS_CONSISTENT_COPY_WRONG_ANNOTATION_TARGET)
            }
            exposedCopy != null && (declaration !is FirRegularClass || !declaration.isData) -> {
                reporter.reportOn(exposedCopy.source, FirErrors.DATA_CLASS_CONSISTENT_COPY_WRONG_ANNOTATION_TARGET)
            }
            else -> {
                if (consistentCopy != null && exposedCopy != null) {
                    reporter.reportOn(
                        exposedCopy.source,
                        FirErrors.DATA_CLASS_CONSISTENT_COPY_AND_EXPOSED_COPY_ARE_INCOMPATIBLE_ANNOTATIONS
                    )
                    reporter.reportOn(
                        consistentCopy.source,
                        FirErrors.DATA_CLASS_CONSISTENT_COPY_AND_EXPOSED_COPY_ARE_INCOMPATIBLE_ANNOTATIONS
                    )
                }

                val primaryConstructor = declaration.primaryConstructorIfAny(context.session)
                val isPrimaryConstructorVisibilityPublic = primaryConstructor?.visibility == Visibilities.Public

                val isConstructorExcludedFromExport =
                    declaration.symbol.isExportedToJs() &&
                            primaryConstructor?.getAnnotationByClassId(StandardClassIds.Annotations.jsExportIgnore, context.session) != null

                val isConstructorVisibilityRespected =
                    LanguageFeature.DataClassCopyRespectsConstructorVisibility.isEnabled()

                if (consistentCopy != null && ((!isConstructorExcludedFromExport && isPrimaryConstructorVisibilityPublic) || isConstructorVisibilityRespected)) {
                    reporter.reportOn(
                        consistentCopy.source,
                        FirErrors.REDUNDANT_ANNOTATION,
                        StandardClassIds.Annotations.ConsistentCopyVisibility
                    )
                }

                if (exposedCopy != null && !isConstructorExcludedFromExport && isPrimaryConstructorVisibilityPublic) {
                    reporter.reportOn(
                        exposedCopy.source,
                        FirErrors.REDUNDANT_ANNOTATION,
                        StandardClassIds.Annotations.ExposedCopyVisibility
                    )
                }
            }
        }
    }
}
