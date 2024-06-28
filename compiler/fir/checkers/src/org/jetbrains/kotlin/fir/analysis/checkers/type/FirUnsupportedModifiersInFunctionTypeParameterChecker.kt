/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.KtNodeTypes.ANNOTATION_ENTRY
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.diagnostics.valOrVarKeyword
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.analysis.checkers.FirModifierList
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtValVarKeywordOwner
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.util.getChildren

object FirUnsupportedModifiersInFunctionTypeParameterChecker : FirFunctionalTypeParameterSyntaxChecker() {

    override fun checkPsiOrLightTree(
        element: FirFunctionTypeParameter,
        source: KtSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        checkModifiers(source, reporter, context)
        checkAnnotations(source, reporter, context)
        checkValOrVarKeyword(source, reporter, context)
    }

    private fun checkValOrVarKeyword(source: KtSourceElement, reporter: DiagnosticReporter, context: CheckerContext) {
        val keyword = when (source) {
            is KtPsiSourceElement ->
                (source.psi as? KtValVarKeywordOwner)?.valOrVarKeyword?.toKtPsiSourceElement()

            is KtLightSourceElement ->
                source.treeStructure.valOrVarKeyword(source.lighterASTNode)?.toKtLightSourceElement(source.treeStructure)

        } ?: return

        reporter.reportOn(
            keyword,
            FirErrors.UNSUPPORTED,
            "val or var on parameter in function type",
            context
        )
    }

    private fun checkModifiers(source: KtSourceElement, reporter: DiagnosticReporter, context: CheckerContext): Boolean {
        val modifiersList = source.getModifierList() ?: return true
        for (modifier in modifiersList.modifiers) {
            reporter.reportOn(
                modifier.source,
                FirErrors.UNSUPPORTED,
                "modifier on parameter in function type",
                context
            )
        }
        return false
    }

    private fun checkAnnotations(source: KtSourceElement, reporter: DiagnosticReporter, context: CheckerContext): Boolean {
        val commonModifiersList = source.getModifierList() ?: return true
        val annotationsSource = when (commonModifiersList) {
            is FirModifierList.FirLightModifierList -> {
                val tree = commonModifiersList.tree
                val children = commonModifiersList.modifierList.getChildren(tree)
                children.filter { it.tokenType == ANNOTATION_ENTRY }.map { it.toKtLightSourceElement(tree) }
            }
            is FirModifierList.FirPsiModifierList -> {
                val children = commonModifiersList.modifierList.node.children()
                children.filter { it.elementType == ANNOTATION_ENTRY }.map { KtRealPsiSourceElement(it.psi) }.toList()
            }
        }
        for (ann in annotationsSource) {
            reporter.reportOn(
                ann,
                FirErrors.UNSUPPORTED,
                "annotation on parameter in function type",
                context
            )
        }
        return false
    }
}