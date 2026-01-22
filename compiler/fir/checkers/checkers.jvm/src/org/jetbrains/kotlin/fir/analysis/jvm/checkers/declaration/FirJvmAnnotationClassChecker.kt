/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirAnnotationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.resolve.AnnotationTargetListForDeprecation
import org.jetbrains.kotlin.resolve.AnnotationTargetLists

object FirJvmAnnotationClassChecker : FirClassChecker(MppCheckerKind.Common) {
    @SymbolInternals
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (declaration.classKind != ClassKind.ANNOTATION_CLASS) return
        val constructor = declaration.primaryConstructorIfAny(context.session)?.fir ?: return
        for (parameter in constructor.valueParameters) {
            val property = parameter.correspondingProperty ?: continue
            checkMember(parameter.symbol, property.symbol)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun checkMember(parameter: FirValueParameterSymbol, property: FirPropertySymbol) {
        // avoid reporting the same annotation twice (for parameter and for property)
        val annotations = (parameter.resolvedAnnotationsWithClassIds + property.resolvedAnnotationsWithClassIds)
            .filter { it.source != null && it.source?.kind is KtRealSourceElementKind }
            .groupBy { it.source }.values.map { it.first() }
        for (annotation in annotations) {
            if (annotation.useSiteTarget != null) continue
            if (annotation.toAnnotationClassIdSafe(context.session) in FirAnnotationChecker.STANDARD_ANNOTATION_IDS_WITHOUT_NECESSARY_MIGRATION) continue

            val targets = annotation.getAllowedAnnotationTargets(context.session) intersect potentialUseSites
            if (targets.size > 1) {
                reporter.reportOn(
                    annotation.source,
                    FirJvmErrors.ANNOTATION_IN_ANNOTATION_PARAMETER_REQUIRES_TARGET,
                    targets.map { it.renderName }
                )
            }
        }
    }

    @OptIn(AnnotationTargetListForDeprecation::class)
    private val potentialUseSites = AnnotationTargetLists.T_MEMBER_PROPERTY_IN_ANNOTATION.allPotentialTargets.toSet()

    val KotlinTarget.renderName: String
        get() = when (this) {
            KotlinTarget.VALUE_PARAMETER -> "param"
            KotlinTarget.PROPERTY -> "property"
            KotlinTarget.PROPERTY_GETTER -> "get"
            KotlinTarget.PROPERTY_SETTER -> "set"
            else -> throw IllegalStateException("Unexpected target: $this")
        }
}