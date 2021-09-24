/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.buildChildSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.*

object FirRedundantLabelChecker : FirDeclarationSyntaxChecker<FirDeclaration, PsiElement>() {
    override fun checkLightTree(
        element: FirDeclaration,
        source: FirLightSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        // Local declarations are already checked when the containing declaration is checked.
        if (!isRootLabelContainer(element)) return

        val allTraversalRoots = mutableSetOf<FirSourceElement>()

        // First collect all labels in the declaration
        element.accept(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                element.acceptChildren(this)
            }

            override fun visitBlock(block: FirBlock) {
                markTraversalRoot(block)
                super.visitBlock(block)
            }

            override fun visitProperty(property: FirProperty) {
                markTraversalRoot(property)
                super.visitProperty(property)
            }

            override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
                markTraversalRoot(simpleFunction)
                super.visitFunction(simpleFunction)
            }

            override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) {
                markTraversalRoot(propertyAccessor)
                super.visitPropertyAccessor(propertyAccessor)
            }

            override fun visitConstructor(constructor: FirConstructor) {
                markTraversalRoot(constructor)
                super.visitConstructor(constructor)
            }

            private fun markTraversalRoot(elem: FirElement) {
                val elemSource = elem.source
                if (elemSource?.kind is FirRealSourceElementKind) {
                    allTraversalRoots.add(elemSource)
                }
            }
        })

        for (root in allTraversalRoots) {
            root.treeStructure.reportRedundantLabels(reporter, context, root as FirLightSourceElement, allTraversalRoots)
        }
    }

    private fun FlyweightCapableTreeStructure<LighterASTNode>.reportRedundantLabels(
        reporter: DiagnosticReporter,
        context: CheckerContext,
        source: FirLightSourceElement,
        allTraversalRoots: Set<FirSourceElement>,
        isChildNode: Boolean = false,
    ) {
        if (isChildNode && source in allTraversalRoots) return // Prevent double traversal
        val node = source.lighterASTNode
        if (node.tokenType == KtNodeTypes.LABELED_EXPRESSION) {
            val labelQualifier = findChildByType(node, KtNodeTypes.LABEL_QUALIFIER)
            if (labelQualifier != null) {
                findChildByType(labelQualifier, KtNodeTypes.LABEL)?.let { labelNode ->
                    when (unwrapParenthesesLabelsAndAnnotations(node).tokenType) {
                        KtNodeTypes.LAMBDA_EXPRESSION, KtNodeTypes.FOR, KtNodeTypes.WHILE, KtNodeTypes.DO_WHILE, KtNodeTypes.FUN -> {}
                        else -> reporter.reportOn(
                            source.buildChildSourceElement(labelNode),
                            FirErrors.REDUNDANT_LABEL_WARNING,
                            context
                        )
                    }
                }
            }
        }
        val childrenRef = Ref.create<Array<LighterASTNode?>>(null)
        getChildren(node, childrenRef)
        for (child in childrenRef.get() ?: return) {
            if (child == null) continue
            reportRedundantLabels(reporter, context, source.buildChildSourceElement(child), allTraversalRoots, true)
        }
    }

    override fun checkPsi(
        element: FirDeclaration,
        source: FirPsiSourceElement,
        psi: PsiElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        // Local declarations are already checked when the containing declaration is checked.
        if (!isRootLabelContainer(element)) return

        // First collect all labels in the declaration
        source.psi.accept(object : KtTreeVisitorVoid() {
            override fun visitLabeledExpression(expression: KtLabeledExpression) {
                val labelNameExpression = expression.getTargetLabel()

                if (labelNameExpression != null) {
                    val deparenthesizedBaseExpression = KtPsiUtil.deparenthesize(expression)
                    if (deparenthesizedBaseExpression !is KtLambdaExpression &&
                        deparenthesizedBaseExpression !is KtLoopExpression &&
                        deparenthesizedBaseExpression !is KtNamedFunction
                    ) {
                        reporter.reportOn(labelNameExpression.toFirPsiSourceElement(), FirErrors.REDUNDANT_LABEL_WARNING, context)
                    }
                }
                super.visitLabeledExpression(expression)
            }
        })
    }

    private fun isRootLabelContainer(element: FirDeclaration): Boolean {
        if (element.source?.kind is FirFakeSourceElementKind) return false
        if (element is FirCallableDeclaration && element.effectiveVisibility == EffectiveVisibility.Local) return false
        return when (element) {
            is FirAnonymousFunction -> false
            is FirFunction -> true
            is FirProperty -> true
            // Consider class initializer of non local class as root label container.
            is FirAnonymousInitializer -> {
                val parentVisibility =
                    (element.getContainingClassSymbol(element.moduleData.session) as? FirRegularClassSymbol)?.effectiveVisibility
                parentVisibility != null && parentVisibility != EffectiveVisibility.Local
            }
            else -> false
        }
    }
}