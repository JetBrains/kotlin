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
import org.jetbrains.kotlin.psi.KtValVarKeywordOwner
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.util.getChildren

object FirUnsupportedModifiersInFunctionTypeParameterChecker : FirFunctionalTypeParameterSyntaxChecker() {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkPsiOrLightTree(
        element: FirFunctionTypeParameter,
        source: KtSourceElement,
    ) {
        checkModifiers(source)
        checkAnnotations(source)
        checkValOrVarKeyword(source)
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkValOrVarKeyword(source: KtSourceElement) {
        val keyword = when (source) {
            is KtPsiSourceElement ->
                (source.psi as? KtValVarKeywordOwner)?.valOrVarKeyword?.toKtPsiSourceElement()

            is KtLightSourceElement ->
                source.treeStructure.valOrVarKeyword(source.lighterASTNode)?.toKtLightSourceElement(source.treeStructure)

        } ?: return

        reporter.reportOn(
            keyword,
            FirErrors.UNSUPPORTED,
            "Function type parameters cannot be 'val' or 'var'."
        )
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkModifiers(source: KtSourceElement): Boolean {
        val modifiersList = source.getModifierList() ?: return true
        for (modifier in modifiersList.modifiers) {
            reporter.reportOn(
                modifier.source,
                FirErrors.UNSUPPORTED,
                "Function type parameters cannot have modifiers."
            )
        }
        return false
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkAnnotations(source: KtSourceElement): Boolean {
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
                "Function type parameters cannot have annotations."
            )
        }
        return false
    }
}