/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget.Companion.classActualTargets
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.resolve.*

object FirModifierChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        // Checked separately
        if (declaration is FirValueParameter && declaration.valueParameterKind == FirValueParameterKind.ContextParameter) return

        val source = when (declaration) {
            is FirFile -> declaration.packageDirective.source
            else -> declaration.source
        }

        if (source == null || source.kind is KtFakeSourceElementKind) {
            return
        }

        source.getModifierList()?.let { checkModifiers(it, declaration) }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkModifiers(
        list: FirModifierList,
        owner: FirDeclaration,
    ) {
        if (list.modifiers.isEmpty()) return

        // general strategy: report no more than one error and any number of warnings
        // therefore, a track of nodes with already reported errors should be kept
        val reportedNodes = hashSetOf<FirModifier<*>>()

        val actualTargets = getActualTargetList(owner).defaultTargets

        val parent = context.findClosest<FirBasedSymbol<*>> {
            !(it is FirConstructorSymbol && it.isPrimary) &&
                    it !is FirPropertySymbol &&
                    it.source?.kind !is KtFakeSourceElementKind
        }

        val actualParents = when (parent) {
            is FirAnonymousObjectSymbol -> KotlinTarget.LOCAL_CLASS_LIST
            is FirClassSymbol -> classActualTargets(
                parent.classKind,
                isInnerClass = parent.isInner,
                isCompanionObject = parent.isCompanion,
                isLocalClass = parent.visibility == Visibilities.Local && parent.isReplSnippetDeclaration != true,
            )
            is FirPropertyAccessorSymbol -> if (parent.isSetter) KotlinTarget.PROPERTY_SETTER_LIST else KotlinTarget.PROPERTY_GETTER_LIST
            is FirFunctionSymbol -> KotlinTarget.FUNCTION_LIST
            is FirEnumEntrySymbol -> KotlinTarget.ENUM_ENTRY_LIST
            else -> KotlinTarget.FILE_LIST
        }

        val modifiers = list.modifiers
        for ((secondIndex, secondModifier) in modifiers.withIndex()) {
            for (firstIndex in 0 until secondIndex) {
                checkCompatibilityType(modifiers[firstIndex], secondModifier, reportedNodes, owner)
            }
            if (secondModifier !in reportedNodes) {
                val modifierSource = secondModifier.source
                val modifier = secondModifier.token
                when {
                    !checkTarget(modifierSource, modifier, actualTargets, parent) -> reportedNodes += secondModifier
                    !checkParent(modifierSource, modifier, actualParents, parent) -> reportedNodes += secondModifier
                }
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkTarget(
        modifierSource: KtSourceElement,
        modifierToken: KtModifierKeywordToken,
        actualTargets: List<KotlinTarget>,
        parent: FirBasedSymbol<*>?,
    ): Boolean {
        fun checkModifier(factory: KtDiagnosticFactory2<KtModifierKeywordToken, String>): Boolean {
            val checkResult = when (factory) {
                FirErrors.WRONG_MODIFIER_TARGET -> actualTargets.none {
                    possibleTargetPredicateMap[modifierToken]?.isAllowed(
                        it,
                        context.session.languageVersionSettings
                    ) == true
                }
                FirErrors.DEPRECATED_MODIFIER_FOR_TARGET -> actualTargets.any {
                    deprecatedTargetPredicateMap[modifierToken]?.isAllowed(
                        it,
                        context.session.languageVersionSettings
                    ) == true
                }
                else -> actualTargets.any { it in (redundantTargetMap[modifierToken] ?: emptySet()) }
            }
            if (checkResult) {
                reporter.reportOn(
                    modifierSource,
                    factory,
                    modifierToken,
                    actualTargets.firstOrThis()
                )
                return false
            }
            return true
        }

        if (!checkModifier(FirErrors.WRONG_MODIFIER_TARGET)) {
            return false
        }

        if (parent is FirRegularClassSymbol && modifierToken == KtTokens.EXPECT_KEYWORD) {
            reporter.reportOn(modifierSource, FirErrors.WRONG_MODIFIER_TARGET, modifierToken, "nested class")
            return false
        }

        if (checkModifier(FirErrors.DEPRECATED_MODIFIER_FOR_TARGET)) {
            checkModifier(FirErrors.REDUNDANT_MODIFIER_FOR_TARGET)
        }

        return true
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkParent(
        modifierSource: KtSourceElement,
        modifierToken: KtModifierKeywordToken,
        actualParents: List<KotlinTarget>,
        parent: FirBasedSymbol<*>?,
    ): Boolean {
        val deprecatedParents = deprecatedParentTargetMap[modifierToken]
        if (deprecatedParents != null && actualParents.any { it in deprecatedParents }) {
            reporter.reportOn(
                modifierSource,
                FirErrors.DEPRECATED_MODIFIER_CONTAINING_DECLARATION,
                modifierToken,
                actualParents.firstOrThis()
            )
            return true
        }

        if (modifierToken == KtTokens.PROTECTED_KEYWORD && isFinalExpectClass(parent)) {
            reporter.reportOn(
                modifierSource,
                FirErrors.WRONG_MODIFIER_CONTAINING_DECLARATION,
                modifierToken,
                "final expect class"
            )
        }
        val possibleParentPredicate = possibleParentTargetPredicateMap[modifierToken] ?: return true
        if (actualParents.any { possibleParentPredicate.isAllowed(it, context.session.languageVersionSettings) }) return true

        if (modifierToken == KtTokens.INNER_KEYWORD && parent is FirScriptSymbol) {
            reporter.reportOn(modifierSource, FirErrors.INNER_ON_TOP_LEVEL_SCRIPT_CLASS)
        } else {
            reporter.reportOn(
                modifierSource,
                FirErrors.WRONG_MODIFIER_CONTAINING_DECLARATION,
                modifierToken,
                actualParents.firstOrThis()
            )
        }

        return false
    }

    private fun List<KotlinTarget>.firstOrThis(): String {
        return firstOrNull()?.description ?: "this"
    }

    private fun isFinalExpectClass(d: FirBasedSymbol<*>?): Boolean {
        return d is FirClassSymbol && d.isFinal && d.isExpect
    }
}
