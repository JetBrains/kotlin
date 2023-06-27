/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget.Companion.classActualTargets
import org.jetbrains.kotlin.fir.analysis.checkers.FirModifier
import org.jetbrains.kotlin.fir.analysis.checkers.FirModifierList
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.getActualTargetList
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.DATA_KEYWORD
import org.jetbrains.kotlin.resolve.*

object FirModifierChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirFile) return

        val source = declaration.source ?: return
        if (source.kind is KtFakeSourceElementKind) return

        source.getModifierList()?.let { checkModifiers(it, declaration, context, reporter) }
    }

    private fun checkModifiers(
        list: FirModifierList,
        owner: FirDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (list.modifiers.isEmpty()) return

        // general strategy: report no more than one error and any number of warnings
        // therefore, a track of nodes with already reported errors should be kept
        val reportedNodes = hashSetOf<FirModifier<*>>()

        val actualTargets = getActualTargetList(owner).defaultTargets

        val parent = context.findClosest<FirDeclaration> {
            it !is FirPrimaryConstructor &&
                    it !is FirProperty &&
                    it.source?.kind !is KtFakeSourceElementKind
        }

        val actualParents = when (parent) {
            is FirAnonymousObject -> KotlinTarget.LOCAL_CLASS_LIST
            is FirClass -> classActualTargets(
                parent.classKind,
                isInnerClass = (parent as? FirMemberDeclaration)?.isInner ?: false,
                isCompanionObject = (parent as? FirRegularClass)?.isCompanion ?: false,
                isLocalClass = parent.isLocal
            )
            is FirPropertyAccessor -> if (parent.isSetter) KotlinTarget.PROPERTY_SETTER_LIST else KotlinTarget.PROPERTY_GETTER_LIST
            is FirFunction -> KotlinTarget.FUNCTION_LIST
            is FirEnumEntry -> KotlinTarget.ENUM_ENTRY_LIST
            else -> KotlinTarget.FILE_LIST
        }

        val modifiers = list.modifiers
        for ((secondIndex, secondModifier) in modifiers.withIndex()) {
            for (firstIndex in 0 until secondIndex) {
                checkCompatibilityType(modifiers[firstIndex], secondModifier, reporter, reportedNodes, owner, context)
            }
            if (secondModifier !in reportedNodes) {
                val modifierSource = secondModifier.source
                val modifier = secondModifier.token
                when {
                    !checkTarget(modifierSource, modifier, actualTargets, parent, context, reporter) -> reportedNodes += secondModifier
                    !checkParent(modifierSource, modifier, actualParents, parent, context, reporter) -> reportedNodes += secondModifier
                }
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

    private fun checkTarget(
        modifierSource: KtSourceElement,
        modifierToken: KtModifierKeywordToken,
        actualTargets: List<KotlinTarget>,
        parent: FirDeclaration?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ): Boolean {
        fun checkModifier(factory: KtDiagnosticFactory2<KtModifierKeywordToken, String>): Boolean {
            val map = when (factory) {
                FirErrors.WRONG_MODIFIER_TARGET -> possibleTargetMap
                FirErrors.DEPRECATED_MODIFIER_FOR_TARGET -> deprecatedTargetMap
                else -> redundantTargetMap
            }
            val set = map[modifierToken] ?: emptySet()
            val checkResult = if (factory == FirErrors.WRONG_MODIFIER_TARGET) {
                actualTargets.none { it in set } ||
                        (modifierToken == DATA_KEYWORD
                                && actualTargets.contains(KotlinTarget.STANDALONE_OBJECT)
                                && !context.languageVersionSettings.supportsFeature(LanguageFeature.DataObjects))
            } else {
                actualTargets.any { it in set }
            }
            if (checkResult) {
                reporter.reportOn(
                    modifierSource,
                    factory,
                    modifierToken,
                    actualTargets.firstOrThis(),
                    context
                )
                return false
            }
            return true
        }

        if (!checkModifier(FirErrors.WRONG_MODIFIER_TARGET)) {
            return false
        }

        if (parent is FirRegularClass) {
            if (modifierToken == KtTokens.EXPECT_KEYWORD || modifierToken == KtTokens.HEADER_KEYWORD) {
                reporter.reportOn(modifierSource, FirErrors.WRONG_MODIFIER_TARGET, modifierToken, "nested class", context)
                return false
            }
        }

        val deprecatedModifierReplacement = deprecatedModifierMap[modifierToken]
        if (deprecatedModifierReplacement != null) {
            reporter.reportOn(
                modifierSource,
                FirErrors.DEPRECATED_MODIFIER,
                modifierToken,
                deprecatedModifierReplacement,
                context
            )
        } else if (checkModifier(FirErrors.DEPRECATED_MODIFIER_FOR_TARGET)) {
            checkModifier(FirErrors.REDUNDANT_MODIFIER_FOR_TARGET)
        }

        return true
    }

    private fun checkParent(
        modifierSource: KtSourceElement,
        modifierToken: KtModifierKeywordToken,
        actualParents: List<KotlinTarget>,
        parent: FirDeclaration?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ): Boolean {
        val deprecatedParents = deprecatedParentTargetMap[modifierToken]
        if (deprecatedParents != null && actualParents.any { it in deprecatedParents }) {
            reporter.reportOn(
                modifierSource,
                FirErrors.DEPRECATED_MODIFIER_CONTAINING_DECLARATION,
                modifierToken,
                actualParents.firstOrThis(),
                context
            )
            return true
        }

        if (modifierToken == KtTokens.PROTECTED_KEYWORD && isFinalExpectClass(parent)) {
            reporter.reportOn(
                modifierSource,
                FirErrors.WRONG_MODIFIER_CONTAINING_DECLARATION,
                modifierToken,
                "final expect class",
                context,
            )
        }
        val possibleParentPredicate = possibleParentTargetPredicateMap[modifierToken] ?: return true
        if (actualParents.any { possibleParentPredicate.isAllowed(it, context.session.languageVersionSettings) }) return true

        if (modifierToken == KtTokens.INNER_KEYWORD && parent is FirScript) {
            reporter.reportOn(modifierSource, FirErrors.INNER_ON_TOP_LEVEL_SCRIPT_CLASS, context)
        } else {
            reporter.reportOn(
                modifierSource,
                FirErrors.WRONG_MODIFIER_CONTAINING_DECLARATION,
                modifierToken,
                actualParents.firstOrThis(),
                context
            )
        }

        return false
    }

    private fun List<KotlinTarget>.firstOrThis(): String {
        return firstOrNull()?.description ?: "this"
    }

    private fun isFinalExpectClass(d: FirDeclaration?): Boolean {
        return d is FirClass && d.isFinal && d.isExpect
    }
}
