/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getTargetAnnotation
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.extractEnumValueArgumentInfo
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.unwrapAndFlattenArgument
import org.jetbrains.kotlin.name.JvmStandardClassIds.Annotations.Java
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.KOTLIN_TO_JAVA_ANNOTATION_TARGETS

object FirIncompatibleAnnotationsChecker : FirClassChecker(MppCheckerKind.Common) {
    override fun check(
        declaration: FirClass,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val javaTarget = declaration.getAnnotationByClassId(Java.Target, context.session) ?: return
        when (val kotlinTarget = declaration.getTargetAnnotation(context.session)) {
            null -> reportIncompatibleTargetsNotSpecified(javaTarget, context, reporter)
            else -> reportIncompatibleTargetsSpecified(kotlinTarget, javaTarget, context, reporter)
        }
    }

    fun reportIncompatibleTargetsNotSpecified(javaTarget: FirAnnotation, context: CheckerContext, reporter: DiagnosticReporter) {
        reporter.reportOn(javaTarget.source, FirJvmErrors.INCOMPATIBLE_ANNOTATION_TARGETS_NOT_SPECIFIED, context)
    }

    fun reportIncompatibleTargetsSpecified(kotlinTarget: FirAnnotation, javaTarget: FirAnnotation, context: CheckerContext, reporter: DiagnosticReporter) {
        val correspondingJavaTargets = kotlinTarget.extractArguments(StandardClassIds.Annotations.ParameterNames.targetAllowedTargets)
            .groupBy { KOTLIN_TO_JAVA_ANNOTATION_TARGETS[it] }.toMutableMap()
        // remove things which are included in the Java @Target annotation
        correspondingJavaTargets.remove(null)
        javaTarget.extractArguments(StandardClassIds.Annotations.ParameterNames.value).forEach { correspondingJavaTargets.remove(it) }

        if (correspondingJavaTargets.isNotEmpty()) {
            reporter.reportOn(
                javaTarget.source,
                FirJvmErrors.INCOMPATIBLE_ANNOTATION_TARGETS_SPECIFIED,
                correspondingJavaTargets.keys.filterNotNull(),
                correspondingJavaTargets.values.flatten(),
                context
            )
        }
    }

    private fun FirAnnotation.extractArguments(
        argumentName: Name
    ): Set<String> = findArgumentByName(argumentName)?.unwrapAndFlattenArgument(flattenArrays = true).orEmpty()
        .mapNotNullTo(mutableSetOf()) { argument ->
            argument.extractEnumValueArgumentInfo()?.enumEntryName?.asString()
        }
}