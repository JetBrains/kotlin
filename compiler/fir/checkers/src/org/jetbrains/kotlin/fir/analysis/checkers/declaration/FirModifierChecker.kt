/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import com.intellij.lang.ASTNode
import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.TokenSet
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtModifierListOwner

object FirModifierChecker : FirBasicDeclarationChecker() {

    private enum class CompatibilityType {
        COMPATIBLE,
        COMPATIBLE_FOR_CLASSES, // for functions and properties: error
        REDUNDANT_1_TO_2,       // first is redundant to second: warning
        REDUNDANT_2_TO_1,       // second is redundant to first: warning
        DEPRECATED,             // pair is deprecated and will soon become incompatible: warning
        REPEATED,               // first and second are the same: error
        INCOMPATIBLE,           // pair is incompatible: error
    }

    // first modifier in pair should also be first in spelling order and declaration's modifier list
    private val compatibilityTypeMap = hashMapOf<Pair<KtModifierKeywordToken, KtModifierKeywordToken>, CompatibilityType>()

    private fun recordCompatibilityType(compatibilityType: CompatibilityType, vararg list: KtModifierKeywordToken) {
        for (firstKeyword in list) {
            for (secondKeyword in list) {
                if (firstKeyword != secondKeyword) {
                    compatibilityTypeMap[Pair(firstKeyword, secondKeyword)] = compatibilityType
                }
            }
        }
    }

    private fun recordPairsCompatibleForClasses(vararg list: KtModifierKeywordToken) {
        recordCompatibilityType(CompatibilityType.COMPATIBLE_FOR_CLASSES, *list)
    }

    private fun recordDeprecatedPairs(vararg list: KtModifierKeywordToken) {
        recordCompatibilityType(CompatibilityType.DEPRECATED, *list)
    }

    private fun recordIncompatiblePairs(vararg list: KtModifierKeywordToken) {
        recordCompatibilityType(CompatibilityType.INCOMPATIBLE, *list)
    }

    // note that order matters: the first argument is redundant to the second, not the other way around
    private fun recordRedundantPairs(redundantKeyword: KtModifierKeywordToken, sufficientKeyword: KtModifierKeywordToken) {
        compatibilityTypeMap[Pair(redundantKeyword, sufficientKeyword)] = CompatibilityType.REDUNDANT_1_TO_2
        compatibilityTypeMap[Pair(sufficientKeyword, redundantKeyword)] = CompatibilityType.REDUNDANT_2_TO_1
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

        recordIncompatiblePairs(PRIVATE_KEYWORD, OVERRIDE_KEYWORD)
        recordPairsCompatibleForClasses(PRIVATE_KEYWORD, OPEN_KEYWORD)
        recordPairsCompatibleForClasses(PRIVATE_KEYWORD, ABSTRACT_KEYWORD)

        // 1. subclasses contained inside a sealed class can not be instantiated, because their constructors needs
        // an instance of an outer sealed (effectively abstract) class
        // 2. subclasses of a non-top-level sealed class must be declared inside the class
        // (see the KEEP https://github.com/Kotlin/KEEP/blob/master/proposals/sealed-class-inheritance.md)
        recordIncompatiblePairs(SEALED_KEYWORD, INNER_KEYWORD)

        recordRedundantPairs(OPEN_KEYWORD, ABSTRACT_KEYWORD)
        recordRedundantPairs(ABSTRACT_KEYWORD, SEALED_KEYWORD)
    }

    private fun deduceCompatibilityType(firstKeyword: KtModifierKeywordToken, secondKeyword: KtModifierKeywordToken): CompatibilityType =
        if (firstKeyword == secondKeyword) {
            CompatibilityType.REPEATED
        } else {
            compatibilityTypeMap[Pair(firstKeyword, secondKeyword)] ?: CompatibilityType.COMPATIBLE
        }

    private fun checkCompatibilityType(
        firstModifier: FirModifier<*>,
        secondModifier: FirModifier<*>,
        reporter: DiagnosticReporter,
        reportedNodes: MutableSet<FirModifier<*>>,
        owner: FirDeclaration?
    ) {
        val firstToken = firstModifier.token
        val secondToken = secondModifier.token
        when (val compatibilityType = deduceCompatibilityType(firstToken, secondToken)) {
            CompatibilityType.COMPATIBLE -> {
            }
            CompatibilityType.REPEATED ->
                if (reportedNodes.add(secondModifier)) reporter.reportRepeatedModifier(secondModifier, secondToken)
            CompatibilityType.REDUNDANT_2_TO_1 ->
                reporter.reportRedundantModifier(secondModifier, secondToken, firstToken)
            CompatibilityType.REDUNDANT_1_TO_2 ->
                reporter.reportRedundantModifier(firstModifier, firstToken, secondToken)
            CompatibilityType.DEPRECATED -> {
                reporter.reportDeprecatedModifierPair(firstModifier, firstToken, secondToken)
                reporter.reportDeprecatedModifierPair(secondModifier, secondToken, firstToken)
            }
            CompatibilityType.INCOMPATIBLE, CompatibilityType.COMPATIBLE_FOR_CLASSES -> {
                if (compatibilityType == CompatibilityType.COMPATIBLE_FOR_CLASSES && owner is FirClass<*>) {
                    return
                }
                if (reportedNodes.add(firstModifier)) reporter.reportIncompatibleModifiers(firstModifier, firstToken, secondToken)
                if (reportedNodes.add(secondModifier)) reporter.reportIncompatibleModifiers(secondModifier, secondToken, firstToken)
            }
        }
    }

    private fun checkModifiers(
        list: FirModifierList,
        owner: FirDeclaration,
        reporter: DiagnosticReporter
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
                checkCompatibilityType(firstModifier, secondModifier, reporter, reportedNodes, owner)
            }
        }
    }

    private fun isDeclarationMappedToSourceCorrectly(declaration: FirDeclaration, source: FirSourceElement): Boolean =
        when (source.elementType) {
            KtNodeTypes.CLASS -> declaration is FirClass<*>
            KtNodeTypes.OBJECT_DECLARATION -> declaration is FirClass<*>
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

        val modifierList = with(FirModifierList) { source.getModifierList() }
        modifierList?.let { checkModifiers(it, declaration, reporter) }
    }

    private fun DiagnosticReporter.reportRepeatedModifier(
        modifier: FirModifier<*>, keyword: KtModifierKeywordToken
    ) {
        report(FirErrors.REPEATED_MODIFIER.on(modifier.source, keyword))
    }

    private fun DiagnosticReporter.reportRedundantModifier(
        modifier: FirModifier<*>, firstKeyword: KtModifierKeywordToken, secondKeyword: KtModifierKeywordToken
    ) {
        report(FirErrors.REDUNDANT_MODIFIER.on(modifier.source, firstKeyword, secondKeyword))
    }

    private fun DiagnosticReporter.reportDeprecatedModifierPair(
        modifier: FirModifier<*>, firstKeyword: KtModifierKeywordToken, secondKeyword: KtModifierKeywordToken
    ) {
        report(FirErrors.DEPRECATED_MODIFIER_PAIR.on(modifier.source, firstKeyword, secondKeyword))
    }

    private fun DiagnosticReporter.reportIncompatibleModifiers(
        modifier: FirModifier<*>, firstKeyword: KtModifierKeywordToken, secondKeyword: KtModifierKeywordToken
    ) {
        report(FirErrors.INCOMPATIBLE_MODIFIERS.on(modifier.source, firstKeyword, secondKeyword))
    }

    private sealed class FirModifierList {
        abstract val modifiers: List<FirModifier<*>>

        class FirPsiModifierList(val modifierList: KtModifierList) : FirModifierList() {
            override val modifiers: List<FirModifier.FirPsiModifier>
                get() = modifierList.node.getChildren(MODIFIER_KEYWORD_SET).map { node ->
                    FirModifier.FirPsiModifier(node, node.elementType as KtModifierKeywordToken)
                }

        }

        class FirLightModifierList(
            val modifierList: LighterASTNode,
            val tree: FlyweightCapableTreeStructure<LighterASTNode>
        ) : FirModifierList() {
            override val modifiers: List<FirModifier.FirLightModifier>
                get() {
                    val modifierNodes = modifierList.getChildren(tree)
                    return modifierNodes.filterNotNull()
                        .filter { it.tokenType is KtModifierKeywordToken }
                        .map { FirModifier.FirLightModifier(it, it.tokenType as KtModifierKeywordToken, tree) }
                }
        }

        companion object {
            fun FirSourceElement?.getModifierList(): FirModifierList? {
                return when (this) {
                    null -> null
                    is FirPsiSourceElement<*> -> (psi as? KtModifierListOwner)?.modifierList?.let { FirPsiModifierList(it) }
                    is FirLightSourceElement -> {
                        val modifierListNode = lighterASTNode.getChildren(treeStructure).find { it?.tokenType == KtNodeTypes.MODIFIER_LIST }
                            ?: return null
                        FirLightModifierList(modifierListNode, treeStructure)
                    }
                }
            }
        }
    }

    private val MODIFIER_KEYWORD_SET = TokenSet.orSet(SOFT_KEYWORDS, TokenSet.create(IN_KEYWORD, FUN_KEYWORD))

    sealed class FirModifier<Node : Any>(val node: Node, val token: KtModifierKeywordToken) {

        class FirPsiModifier(
            node: ASTNode,
            token: KtModifierKeywordToken
        ) : FirModifier<ASTNode>(node, token) {
            override val source: FirSourceElement
                get() = node.psi.toFirPsiSourceElement()
        }

        class FirLightModifier(
            node: LighterASTNode,
            token: KtModifierKeywordToken,
            val tree: FlyweightCapableTreeStructure<LighterASTNode>
        ) : FirModifier<LighterASTNode>(node, token) {
            override val source: FirSourceElement
                get() = node.toFirLightSourceElement(tree)
        }

        abstract val source: FirSourceElement
    }
}
