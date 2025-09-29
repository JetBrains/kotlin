/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getTargetAnnotation
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.extractEnumValueArgumentInfo
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.unwrapAndFlattenArgument
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.ParameterNames

object FirDslMarkerTargetChecker : FirRegularClassChecker(MppCheckerKind.Platform) {
    private val VALID_NAMES = setOf(
        AnnotationTarget.CLASS.name,
        AnnotationTarget.TYPE.name,
        AnnotationTarget.TYPEALIAS.name,
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        if (declaration.classKind != ClassKind.ANNOTATION_CLASS) return
        if (!declaration.hasAnnotation(StandardClassIds.Annotations.DslMarker, context.session)) return

        val targetAnnotation = declaration.getTargetAnnotation(context.session)

        if (targetAnnotation == null) {
            reporter.reportOn(declaration.source, FirErrors.DSL_MARKER_WITH_DEFAULT_TARGETS)
        } else {
            val arguments = targetAnnotation
                .findArgumentByName(ParameterNames.targetAllowedTargets)
                ?.unwrapAndFlattenArgument(flattenArrays = true)
                ?: return

            for (argument in arguments) {
                val name = argument.extractEnumValueArgumentInfo()?.enumEntryName?.identifierOrNullIfSpecial ?: continue
                if (name !in VALID_NAMES) {
                    reporter.reportOn(argument.source, FirErrors.WRONG_DSL_MARKER_TARGET)
                }
            }
        }
    }
}
