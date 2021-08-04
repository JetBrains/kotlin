/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget.*
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget.Companion.classActualTargets
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticFactory2
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.hasBody
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.resolve.*

object FirModifierChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirFile) return

        val source = declaration.source ?: return
        if (source.kind is FirFakeSourceElementKind) return

        if (declaration is FirProperty) {
            fun checkPropertyAccessor(propertyAccessor: FirPropertyAccessor?) {
                if (propertyAccessor != null && !propertyAccessor.hasBody) {
                    check(propertyAccessor, context, reporter)
                }
            }

            checkPropertyAccessor(declaration.getter)
            checkPropertyAccessor(declaration.setter)
        }

        source.getModifierList()?.let { checkModifiers(it, declaration, context, reporter) }
    }

    private fun checkModifiers(
        list: FirModifierList,
        owner: FirDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        // general strategy: report no more than one error and any number of warnings
        // therefore, a track of nodes with already reported errors should be kept
        val reportedNodes = hashSetOf<FirModifier<*>>()
        val actualTargets = getActualTargetList(owner).defaultTargets

        val parent = context.findClosest<FirDeclaration> {
            it !is FirPrimaryConstructor &&
                    it !is FirProperty &&
                    it.source?.kind !is FirFakeSourceElementKind
        }

        val actualParents = when (parent) {
            is FirAnonymousObject -> listOf(LOCAL_CLASS, CLASS)
            is FirClass -> classActualTargets(
                parent.classKind,
                isInnerClass = (parent as? FirMemberDeclaration)?.isInner ?: false,
                isCompanionObject = (parent as? FirRegularClass)?.isCompanion ?: false,
                isLocalClass = parent.isLocal
            )
            is FirPropertyAccessor -> listOf(if (parent.isSetter) PROPERTY_SETTER else PROPERTY_GETTER)
            is FirFunction -> listOf(FUNCTION)
            is FirEnumEntry -> listOf(ENUM_ENTRY, PROPERTY, FIELD)
            else -> listOf(FILE)
        }

        val modifiers = list.modifiers
        for ((secondIndex, secondModifier) in modifiers.withIndex()) {
            for (firstIndex in 0 until secondIndex) {
                checkCompatibilityType(modifiers[firstIndex], secondModifier, reporter, reportedNodes, owner, context)
            }
            if (secondModifier !in reportedNodes) {
                val modifierSource = secondModifier.source
                val modifierType = getKeywordType(secondModifier)
                when {
                    !checkTarget(modifierSource, modifierType, actualTargets, parent, context, reporter) -> reportedNodes += secondModifier
                    !checkParent(modifierSource, modifierType, actualParents, context, reporter) -> reportedNodes += secondModifier
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

    private fun checkTarget(
        modifierSource: FirSourceElement,
        modifierType: KeywordType,
        actualTargets: List<KotlinTarget>,
        parent: FirDeclaration?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ): Boolean {
        fun checkModifier(factory: FirDiagnosticFactory2<String, String>): Boolean {
            val map = when (factory) {
                FirErrors.WRONG_MODIFIER_TARGET -> possibleTargetMap
                FirErrors.DEPRECATED_MODIFIER_FOR_TARGET -> deprecatedTargetMap
                else -> redundantTargetMap
            }
            val set = map[modifierType] ?: emptySet()
            val checkResult = if (factory == FirErrors.WRONG_MODIFIER_TARGET) {
                actualTargets.none { it in set }
            } else {
                actualTargets.any { it in set }
            }
            if (checkResult) {
                reporter.reportOn(
                    modifierSource,
                    factory,
                    modifierType.render(),
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
            if (modifierType == KeywordType.Expect || modifierType == KeywordType.Header) {
                reporter.reportOn(modifierSource, FirErrors.WRONG_MODIFIER_TARGET, modifierType.render(), "nested class", context)
                return false
            }
        }

        val deprecatedModifierReplacement = deprecatedModifierMap[modifierType]
        if (deprecatedModifierReplacement != null) {
            reporter.reportOn(
                modifierSource,
                FirErrors.DEPRECATED_MODIFIER,
                modifierType.render(),
                deprecatedModifierReplacement.render(),
                context
            )
        } else if (checkModifier(FirErrors.DEPRECATED_MODIFIER_FOR_TARGET)) {
            checkModifier(FirErrors.REDUNDANT_MODIFIER_FOR_TARGET)
        }

        return true
    }

    private fun checkParent(
        modifierSource: FirSourceElement,
        modifierType: KeywordType,
        actualParents: List<KotlinTarget>,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ): Boolean {
        val deprecatedParents = deprecatedParentTargetMap[modifierType]
        if (deprecatedParents != null && actualParents.any { it in deprecatedParents }) {
            reporter.reportOn(
                modifierSource,
                FirErrors.DEPRECATED_MODIFIER_CONTAINING_DECLARATION,
                modifierType.render(),
                actualParents.firstOrThis(),
                context
            )
            return true
        }

        val possibleParentPredicate = possibleParentTargetPredicateMap[modifierType] ?: return true
        if (actualParents.any { possibleParentPredicate.isAllowed(it, context.session.languageVersionSettings) }) return true

        reporter.reportOn(
            modifierSource,
            FirErrors.WRONG_MODIFIER_CONTAINING_DECLARATION,
            modifierType.render(),
            actualParents.firstOrThis(),
            context
        )

        return false
    }

    private fun List<KotlinTarget>.firstOrThis(): String {
        return firstOrNull()?.description ?: "this"
    }
}
