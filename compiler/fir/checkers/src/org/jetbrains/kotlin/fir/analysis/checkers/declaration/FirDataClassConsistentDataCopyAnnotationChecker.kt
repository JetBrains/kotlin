/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.doesDataClassCopyRespectConstructorVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.fqName

object FirDataClassConsistentDataCopyAnnotationChecker : FirClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val consistentCopy =
            declaration.annotations.firstOrNull { it.fqName(context.session) == StandardNames.CONSISTENT_DATA_COPY_VISIBILITY }
        val inconsistentCopy =
            declaration.annotations.firstOrNull { it.fqName(context.session) == StandardNames.INCONSISTENT_DATA_COPY_VISIBILITY }

        when {
            consistentCopy != null && (declaration !is FirRegularClass || !declaration.isData) -> {
                reporter.reportOn(consistentCopy.source, FirErrors.DATA_CLASS_CONSISTENT_COPY_WRONG_ANNOTATION_TARGET, context)
            }
            inconsistentCopy != null && (declaration !is FirRegularClass || !declaration.isData) -> {
                reporter.reportOn(inconsistentCopy.source, FirErrors.DATA_CLASS_CONSISTENT_COPY_WRONG_ANNOTATION_TARGET, context)
            }
            else -> {
                if (consistentCopy != null && inconsistentCopy != null) {
                    reporter.reportOn(
                        inconsistentCopy.source,
                        FirErrors.DATA_CLASS_CONSISTENT_COPY_AND_INCONSISTENT_COPY_ARE_INCOMPATIBLE_ANNOTATIONS,
                        context
                    )
                    reporter.reportOn(
                        consistentCopy.source,
                        FirErrors.DATA_CLASS_CONSISTENT_COPY_AND_INCONSISTENT_COPY_ARE_INCOMPATIBLE_ANNOTATIONS,
                        context
                    )
                }

                if (consistentCopy != null && (context.languageVersionSettings.doesDataClassCopyRespectConstructorVisibility() ||
                            declaration.primaryConstructorIfAny(context.session)?.visibility == Visibilities.Public)
                ) {
                    reporter.reportOn(
                        consistentCopy.source,
                        FirErrors.REDUNDANT_ANNOTATION,
                        StandardNames.CONSISTENT_DATA_COPY_VISIBILITY,
                        context
                    )
                }
            }
        }
    }
}
