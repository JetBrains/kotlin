/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.resolve.Compatibility
import org.jetbrains.kotlin.resolve.compatibility


internal fun checkCompatibilityType(
    firstModifier: FirModifier<*>,
    secondModifier: FirModifier<*>,
    reporter: DiagnosticReporter,
    reportedNodes: MutableSet<FirModifier<*>>,
    owner: FirElement?,
    context: CheckerContext
) {
    val firstModifierToken = firstModifier.token
    val secondModifierToken = secondModifier.token
    when (val compatibilityType = compatibility(firstModifierToken, secondModifierToken)) {
        Compatibility.COMPATIBLE -> {
        }
        Compatibility.REPEATED ->
            if (reportedNodes.add(secondModifier)) {
                reporter.reportOn(secondModifier.source, FirErrors.REPEATED_MODIFIER, secondModifierToken, context)
            }
        Compatibility.REDUNDANT -> {
            reporter.reportOn(
                secondModifier.source,
                FirErrors.REDUNDANT_MODIFIER,
                secondModifierToken,
                firstModifierToken,
                context
            )
        }
        Compatibility.REVERSE_REDUNDANT -> {
            reporter.reportOn(
                firstModifier.source,
                FirErrors.REDUNDANT_MODIFIER,
                firstModifierToken,
                secondModifierToken,
                context
            )
        }
        Compatibility.DEPRECATED -> {
            reporter.reportOn(
                firstModifier.source,
                FirErrors.DEPRECATED_MODIFIER_PAIR,
                firstModifierToken,
                secondModifierToken,
                context
            )
            reporter.reportOn(
                secondModifier.source,
                FirErrors.DEPRECATED_MODIFIER_PAIR,
                secondModifierToken,
                firstModifierToken,
                context
            )
        }
        Compatibility.INCOMPATIBLE, Compatibility.COMPATIBLE_FOR_CLASSES_ONLY -> {
            if (compatibilityType == Compatibility.COMPATIBLE_FOR_CLASSES_ONLY && owner is FirClass) {
                return
            }
            if (reportedNodes.add(firstModifier)) {
                reporter.reportOn(
                    firstModifier.source,
                    FirErrors.INCOMPATIBLE_MODIFIERS,
                    firstModifierToken,
                    secondModifierToken,
                    context
                )
            }
            if (reportedNodes.add(secondModifier)) {
                reporter.reportOn(
                    secondModifier.source,
                    FirErrors.INCOMPATIBLE_MODIFIERS,
                    secondModifierToken,
                    firstModifierToken,
                    context
                )
            }
        }
    }
}

private fun checkModifiersCompatibility(
    owner: FirElement,
    modifierList: FirModifierList,
    reporter: DiagnosticReporter,
    reportedNodes: MutableSet<FirModifier<*>>,
    context: CheckerContext,
) {
    val modifiers = modifierList.modifiers
    for ((secondIndex, secondModifier) in modifiers.withIndex()) {
        for (firstIndex in 0..<secondIndex) {
            checkCompatibilityType(modifiers[firstIndex], secondModifier, reporter, reportedNodes, owner, context)
        }
    }
}

fun checkModifiersCompatibility(typeArgument: FirTypeProjection, context: CheckerContext, reporter: DiagnosticReporter) {
    val source = typeArgument.source?.takeIf { it.kind is KtRealSourceElementKind } ?: return
    val modifierList = source.getModifierList() ?: return

    // general strategy: report no more than one error and any number of warnings
    // therefore, a track of nodes with already reported errors should be kept
    val reportedNodes = hashSetOf<FirModifier<*>>()

    checkModifiersCompatibility(typeArgument, modifierList, reporter, reportedNodes, context)
}
