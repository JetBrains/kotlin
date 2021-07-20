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
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.resolve.hasExposingGetter
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens.*

private fun <T> forPairs(list: List<T>, action: (Pair<T, T>) -> Unit) {
    for (it in list.indices) {
        for (that in it + 1 until list.size) {
            action(list[it] to list[that])
        }
    }
}

private fun <T> forPairs(array: Array<T>, action: (Pair<T, T>) -> Unit) {
    return forPairs(array.asList(), action)
}

private fun <T> Pair<T, T>.reverse() = second to first

private fun mixtureOf(vararg integers: Int) = integers.reduce { accumulator, next -> accumulator or next }

private infix fun Int.includes(mode: Int): Boolean {
    // this == 0 is also some state,
    // but it's not denoted by its
    // own flag and so can't be detected
    // via bitwise and
    return if (mode == 0) {
        this == 0
    } else {
        this and mode != 0
    }
}

@Suppress("PropertyName")
private open class FlagProvider {
    private var nextFlag = 1

    protected fun generateFlag(): Int {
        val flag = nextFlag
        nextFlag = nextFlag shl 1
        return flag
    }

    val NONE = 0

    val ALL_AT_ONCE: Int
        get() = nextFlag - 1
}

private object CompatibleEntities : FlagProvider() {
    val CLASSES = generateFlag()
    val PROPERTIES_WITH_EXPOSING_GETTERS = generateFlag()
    val OTHER = generateFlag()
}

private object IncompatibilityReasons : FlagProvider() {
    val REDUNDANT_1_TO_2 = generateFlag() // first is redundant to second: warning
    val REDUNDANT_2_TO_1 = generateFlag() // second is redundant to first: warning
    val DEPRECATED = generateFlag()       // pair is deprecated and will soon become incompatible: warning
    val REPEATED = generateFlag()         // first and second are the same: error
}

private class CompatibilityInfo {
    var compatibleEntities = CompatibleEntities.ALL_AT_ONCE
    var incompatibilityReasons = IncompatibilityReasons.NONE
}

object FirModifierChecker : FirBasicDeclarationChecker() {
    // first modifier in pair should also be first in spelling order and declaration's modifier list
    private val compatibilityTypeMap = hashMapOf<Pair<KtModifierKeywordToken, KtModifierKeywordToken>, CompatibilityInfo>()

    private fun makeModifiersPairs(
        vararg array: KtModifierKeywordToken,
        configure: CompatibilityInfo.() -> Unit
    ) {
        forPairs(array) { pair ->
            // must be symmetric
            val compatibility = CompatibilityInfo().apply(configure)
            val reversePair = pair.reverse()

            compatibilityTypeMap[pair] = compatibility
            compatibilityTypeMap[reversePair] = compatibility
        }
    }

    private fun recordIncompatiblePairs(vararg list: KtModifierKeywordToken) {
        makeModifiersPairs(*list) {
            compatibleEntities = CompatibleEntities.NONE
        }
    }

    // note that order matters: the first argument is redundant to the second, not the other way around
    private fun recordRedundantPairs(redundantKeyword: KtModifierKeywordToken, sufficientKeyword: KtModifierKeywordToken) {
        compatibilityTypeMap[Pair(redundantKeyword, sufficientKeyword)] = CompatibilityInfo().apply {
            incompatibilityReasons = IncompatibilityReasons.REDUNDANT_1_TO_2
        }
        compatibilityTypeMap[Pair(sufficientKeyword, redundantKeyword)] = CompatibilityInfo().apply {
            incompatibilityReasons = IncompatibilityReasons.REDUNDANT_2_TO_1
        }
    }

    // building the compatibility type mapping
    init {
        recordIncompatiblePairs(IN_KEYWORD, OUT_KEYWORD) // Variance
        recordIncompatiblePairs(PRIVATE_KEYWORD, PROTECTED_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD) // Visibilities
        recordIncompatiblePairs(HEADER_KEYWORD, EXPECT_KEYWORD, IMPL_KEYWORD, ACTUAL_KEYWORD)
        recordIncompatiblePairs(FINAL_KEYWORD, ABSTRACT_KEYWORD)
        recordIncompatiblePairs(FINAL_KEYWORD, OPEN_KEYWORD, SEALED_KEYWORD)
        recordIncompatiblePairs(CROSSINLINE_KEYWORD, NOINLINE_KEYWORD)

        recordIncompatiblePairs(DATA_KEYWORD, OPEN_KEYWORD)
        recordIncompatiblePairs(DATA_KEYWORD, INNER_KEYWORD)
        recordIncompatiblePairs(DATA_KEYWORD, ABSTRACT_KEYWORD)
        recordIncompatiblePairs(DATA_KEYWORD, SEALED_KEYWORD)
        recordIncompatiblePairs(DATA_KEYWORD, INLINE_KEYWORD)

        recordIncompatiblePairs(CONST_KEYWORD, ABSTRACT_KEYWORD)
        recordIncompatiblePairs(CONST_KEYWORD, OPEN_KEYWORD)
        recordIncompatiblePairs(CONST_KEYWORD, OVERRIDE_KEYWORD)

        makeModifiersPairs(PRIVATE_KEYWORD, OVERRIDE_KEYWORD) {
            compatibleEntities = CompatibleEntities.PROPERTIES_WITH_EXPOSING_GETTERS
        }
        makeModifiersPairs(PRIVATE_KEYWORD, OPEN_KEYWORD) {
            compatibleEntities = mixtureOf(
                CompatibleEntities.CLASSES,
                CompatibleEntities.PROPERTIES_WITH_EXPOSING_GETTERS,
            )
        }
        makeModifiersPairs(PRIVATE_KEYWORD, ABSTRACT_KEYWORD) {
            compatibleEntities = mixtureOf(
                CompatibleEntities.CLASSES,
                CompatibleEntities.PROPERTIES_WITH_EXPOSING_GETTERS,
            )
        }

        // 1. subclasses contained inside a sealed class can not be instantiated, because their constructors needs
        // an instance of an outer sealed (effectively abstract) class
        // 2. subclasses of a non-top-level sealed class must be declared inside the class
        // (see the KEEP https://github.com/Kotlin/KEEP/blob/master/proposals/sealed-class-inheritance.md)
        recordIncompatiblePairs(SEALED_KEYWORD, INNER_KEYWORD)

        recordRedundantPairs(OPEN_KEYWORD, ABSTRACT_KEYWORD)
        recordRedundantPairs(ABSTRACT_KEYWORD, SEALED_KEYWORD)
    }

    private fun deduceCompatibilityType(
        firstKeyword: KtModifierKeywordToken,
        secondKeyword: KtModifierKeywordToken
    ): CompatibilityInfo {
        return if (firstKeyword == secondKeyword) {
            CompatibilityInfo().apply {
                incompatibilityReasons = IncompatibilityReasons.REPEATED
            }
        } else {
            compatibilityTypeMap[Pair(firstKeyword, secondKeyword)] ?: CompatibilityInfo()
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
        val firstToken = firstModifier.token
        val secondToken = secondModifier.token
        val compatibilityType = deduceCompatibilityType(firstToken, secondToken)
        when {
            compatibilityType.compatibleEntities == CompatibleEntities.NONE -> {
                reportIncompatiblePair(firstModifier, secondModifier, reporter, reportedNodes, context)
            }
            owner?.isAppropriateFor(compatibilityType) == true -> {
                reportIncompatiblePair(firstModifier, secondModifier, reporter, reportedNodes, context)
            }
            compatibilityType.incompatibilityReasons includes IncompatibilityReasons.REPEATED -> {
                if (reportedNodes.add(secondModifier)) {
                    reporter.reportRepeatedModifier(secondModifier, secondToken, context)
                }
            }
            compatibilityType.incompatibilityReasons includes IncompatibilityReasons.REDUNDANT_2_TO_1 -> {
                reporter.reportRedundantModifier(secondModifier, secondToken, firstToken, context)
            }
            compatibilityType.incompatibilityReasons includes IncompatibilityReasons.REDUNDANT_1_TO_2 -> {
                reporter.reportRedundantModifier(firstModifier, firstToken, secondToken, context)
            }
            compatibilityType.incompatibilityReasons includes IncompatibilityReasons.DEPRECATED -> {
                reporter.reportDeprecatedModifierPair(firstModifier, firstToken, secondToken, context)
                reporter.reportDeprecatedModifierPair(secondModifier, secondToken, firstToken, context)
            }
        }
    }

    private fun FirDeclaration.isAppropriateFor(compatibilityType: CompatibilityInfo): Boolean {
        return when (this) {
            is FirClass -> hasIssuesAsClass(compatibilityType)
            is FirProperty -> this.hasIssuesWithExposingGetter(compatibilityType)
            else -> !(compatibilityType.compatibleEntities includes CompatibleEntities.OTHER)
        }
    }

    private fun hasIssuesAsClass(compatibilityType: CompatibilityInfo): Boolean {
        return !(compatibilityType.compatibleEntities includes CompatibleEntities.CLASSES)
    }

    private fun FirDeclaration.hasIssuesWithExposingGetter(compatibilityType: CompatibilityInfo): Boolean {
        val allIsOk = if (hasExposingGetter()) {
            compatibilityType.compatibleEntities includes CompatibleEntities.PROPERTIES_WITH_EXPOSING_GETTERS
        } else {
            compatibilityType.compatibleEntities includes CompatibleEntities.OTHER
        }
        return !allIsOk
    }

    private fun reportIncompatiblePair(
        firstModifier: FirModifier<*>,
        secondModifier: FirModifier<*>,
        reporter: DiagnosticReporter,
        reportedNodes: MutableSet<FirModifier<*>>,
        context: CheckerContext,
    ) {
        if (reportedNodes.add(firstModifier)) {
            reporter.reportIncompatibleModifiers(firstModifier, firstModifier.token, secondModifier.token, context)
        }
        if (reportedNodes.add(secondModifier)) {
            reporter.reportIncompatibleModifiers(secondModifier, secondModifier.token, firstModifier.token, context)
        }
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

        forPairs(list.modifiers) {
            checkCompatibilityType(it.first, it.second, reporter, reportedNodes, owner, context)
        }
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

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirFile) return

        val source = declaration.source ?: return
        if (!isDeclarationMappedToSourceCorrectly(declaration, source)) return
        if (context.containingDeclarations.last() is FirDefaultPropertyAccessor) return

        val modifierList = source.getModifierList()
        modifierList?.let { checkModifiers(it, declaration, reporter, context) }
    }

    private fun DiagnosticReporter.reportRepeatedModifier(
        modifier: FirModifier<*>, keyword: KtModifierKeywordToken, context: CheckerContext
    ) {
        reportOn(modifier.source, FirErrors.REPEATED_MODIFIER, keyword, context)
    }

    private fun DiagnosticReporter.reportRedundantModifier(
        modifier: FirModifier<*>, firstKeyword: KtModifierKeywordToken, secondKeyword: KtModifierKeywordToken, context: CheckerContext
    ) {
        reportOn(modifier.source, FirErrors.REDUNDANT_MODIFIER, firstKeyword, secondKeyword, context)
    }

    private fun DiagnosticReporter.reportDeprecatedModifierPair(
        modifier: FirModifier<*>, firstKeyword: KtModifierKeywordToken, secondKeyword: KtModifierKeywordToken, context: CheckerContext
    ) {
        reportOn(modifier.source, FirErrors.DEPRECATED_MODIFIER_PAIR, firstKeyword, secondKeyword, context)
    }

    private fun DiagnosticReporter.reportIncompatibleModifiers(
        modifier: FirModifier<*>, firstKeyword: KtModifierKeywordToken, secondKeyword: KtModifierKeywordToken, context: CheckerContext
    ) {
        reportOn(modifier.source, FirErrors.INCOMPATIBLE_MODIFIERS, firstKeyword, secondKeyword, context)
    }
}
