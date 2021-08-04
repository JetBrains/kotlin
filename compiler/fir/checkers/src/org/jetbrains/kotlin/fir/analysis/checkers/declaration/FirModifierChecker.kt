/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.FirModifier
import org.jetbrains.kotlin.fir.analysis.checkers.FirModifierList
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.fir.analysis.checkers.getKeywordType
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.resolve.Compatibility
import org.jetbrains.kotlin.resolve.KeywordType
import org.jetbrains.kotlin.resolve.compatibility

object FirModifierChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirFile) return

        val source = declaration.source ?: return
        if (!isDeclarationMappedToSourceCorrectly(declaration, source)) return
        if (context.containingDeclarations.last() is FirDefaultPropertyAccessor) return

        val modifierList = source.getModifierList()
        modifierList?.let { checkModifiers(it, declaration, reporter, context) }
    }

    private fun isDeclarationMappedToSourceCorrectly(declaration: FirDeclaration, source: FirSourceElement): Boolean =
        when (source.elementType) {
            KtNodeTypes.CLASS -> declaration is FirClass
            KtNodeTypes.OBJECT_DECLARATION -> declaration is FirClass
            KtNodeTypes.PROPERTY -> declaration is FirProperty
            KtNodeTypes.VALUE_PARAMETER -> declaration is FirValueParameter
            // TODO more FIR-PSI relations possibly have to be added
            else -> true
        }

    private fun checkModifiers(
        list: FirModifierList,
        owner: FirDeclaration,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        // general strategy: report no more than one error and any number of warnings
        // therefore, a track of nodes with already reported errors should be kept
        val reportedNodes = hashSetOf<FirModifier<*>>()

        val modifiers = list.modifiers
        for (secondModifier in modifiers) {
            for (firstModifier in modifiers) {
                if (firstModifier == secondModifier) {
                    break
                }
                checkCompatibilityType(firstModifier, secondModifier, reporter, reportedNodes, owner, context)
            }
        }
    }

    private fun checkCompatibilityType(
        firstModifier: FirModifier<*>,
        secondModifier: FirModifier<*>,
        reporter: DiagnosticReporter,
        reportedNodes: MutableSet<FirModifier<*>>,
        owner: FirDeclaration?,
        context: CheckerContext
    ) {
        val firstModifierType = getKeywordType(firstModifier)
        val secondModifierType = getKeywordType(secondModifier)
        when (val compatibilityType = compatibility(firstModifierType, secondModifierType)) {
            Compatibility.COMPATIBLE -> {
            }
            Compatibility.REPEATED ->
                if (reportedNodes.add(secondModifier)) {
                    reporter.reportOn(secondModifier.source, FirErrors.REPEATED_MODIFIER, secondModifierType.render(), context)
                }
            Compatibility.REDUNDANT -> {
                reporter.reportOn(
                    secondModifier.source,
                    FirErrors.REDUNDANT_MODIFIER,
                    secondModifierType.render(),
                    firstModifierType.render(),
                    context
                )
            }
            Compatibility.REVERSE_REDUNDANT -> {
                reporter.reportOn(
                    firstModifier.source,
                    FirErrors.REDUNDANT_MODIFIER,
                    firstModifierType.render(),
                    secondModifierType.render(),
                    context
                )
            }
            Compatibility.DEPRECATED -> {
                reporter.reportOn(
                    firstModifier.source,
                    FirErrors.DEPRECATED_MODIFIER_PAIR,
                    firstModifierType.render(),
                    secondModifierType.render(),
                    context
                )
                reporter.reportOn(
                    secondModifier.source,
                    FirErrors.DEPRECATED_MODIFIER_PAIR,
                    secondModifierType.render(),
                    firstModifierType.render(),
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
                        firstModifierType.render(),
                        secondModifierType.render(),
                        context
                    )
                }
                if (reportedNodes.add(secondModifier)) {
                    reporter.reportOn(
                        secondModifier.source,
                        FirErrors.INCOMPATIBLE_MODIFIERS,
                        secondModifierType.render(),
                        firstModifierType.render(),
                        context
                    )
                }
            }
        }
    }

    private fun KeywordType.render(): String {
        return this.toString().lowercase()
    }
}
