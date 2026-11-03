/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.syntax.FirModifier
import org.jetbrains.kotlin.fir.syntax.FirModifierList
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.resolve.Compatibility
import org.jetbrains.kotlin.resolve.compatibility


context(reporter: DiagnosticReporter, context: CheckerContext)
internal fun checkCompatibilityType(
    firstModifier: FirModifier,
    secondModifier: FirModifier,
    reportedNodes: MutableSet<FirModifier>,
    owner: FirElement?
) {
    when (val compatibilityType = compatibility(firstModifier.tokenId, secondModifier.tokenId)) {
        Compatibility.COMPATIBLE -> {
        }
        Compatibility.REPEATED ->
            if (reportedNodes.add(secondModifier)) {
                reporter.reportOn(secondModifier.source, FirErrors.REPEATED_MODIFIER, secondModifier.tokenName)
            }
        Compatibility.REDUNDANT -> {
            reporter.reportOn(
                secondModifier.source,
                FirErrors.REDUNDANT_MODIFIER,
                secondModifier.tokenName,
                firstModifier.tokenName
            )
        }
        Compatibility.REVERSE_REDUNDANT -> {
            reporter.reportOn(
                firstModifier.source,
                FirErrors.REDUNDANT_MODIFIER,
                firstModifier.tokenName,
                secondModifier.tokenName
            )
        }
        Compatibility.DEPRECATED -> {
            reporter.reportOn(
                firstModifier.source,
                FirErrors.DEPRECATED_MODIFIER_PAIR,
                firstModifier.tokenName,
                secondModifier.tokenName
            )
            reporter.reportOn(
                secondModifier.source,
                FirErrors.DEPRECATED_MODIFIER_PAIR,
                secondModifier.tokenName,
                firstModifier.tokenName
            )
        }
        Compatibility.INLINE_VALUE_ALLOWED if (LanguageFeature.ExplicitInlineValueClassesSyntax.isEnabled()) -> {
            return
        }
        Compatibility.INCOMPATIBLE, Compatibility.COMPATIBLE_FOR_CLASSES_ONLY, Compatibility.INLINE_VALUE_ALLOWED -> {
            if (compatibilityType == Compatibility.COMPATIBLE_FOR_CLASSES_ONLY && owner is FirClass) {
                return
            }
            if (reportedNodes.add(firstModifier)) {
                reporter.reportOn(
                    firstModifier.source,
                    FirErrors.INCOMPATIBLE_MODIFIERS,
                    firstModifier.tokenName,
                    secondModifier.tokenName
                )
            }
            if (reportedNodes.add(secondModifier)) {
                reporter.reportOn(
                    secondModifier.source,
                    FirErrors.INCOMPATIBLE_MODIFIERS,
                    secondModifier.tokenName,
                    firstModifier.tokenName
                )
            }
        }
    }
}

context(reporter: DiagnosticReporter, context: CheckerContext)
private fun checkModifiersCompatibility(
    owner: FirElement,
    modifierList: FirModifierList,
    reportedNodes: MutableSet<FirModifier>,
) {
    val modifiers = modifierList.modifiers
    for ([secondIndex, secondModifier] in modifiers.withIndex()) {
        for (firstIndex in 0..<secondIndex) {
            checkCompatibilityType(modifiers[firstIndex], secondModifier, reportedNodes, owner)
        }
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkModifiersCompatibility(typeArgument: FirTypeProjection) {
    val source = typeArgument.source?.takeIf { it.kind is KtRealSourceElementKind } ?: return
    val modifierList = source.getModifierList() ?: return

    // general strategy: report no more than one error and any number of warnings
    // therefore, a track of nodes with already reported errors should be kept
    val reportedNodes = hashSetOf<FirModifier>()

    checkModifiersCompatibility(typeArgument, modifierList, reportedNodes)
}
