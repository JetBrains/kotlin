/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes.TYPE_ARGUMENT_LIST
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.KtTypeArgumentList

object FirTypeArgumentsNotAllowedExpressionChecker : FirQualifiedAccessChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // analyze type parameters near
        // package names
        val explicitReceiver = expression.explicitReceiver

        if (explicitReceiver is FirResolvedQualifier && explicitReceiver.symbol == null) {
            if (explicitReceiver.source?.hasAnyArguments() == true) {
                reporter.report(explicitReceiver.source)
                return
            }
        }
    }

    private fun FirSourceElement.hasAnyArguments(): Boolean {
        val localPsi = this.psi
        val localLight = this.lighterASTNode

        if (localPsi != null && localPsi !is PsiErrorElement) {
            return localPsi.hasAnyArguments()
        } else if (this is FirLightSourceElement) {
            return localLight.hasAnyArguments(this.treeStructure)
        }

        return false
    }

    private fun PsiElement.hasAnyArguments(): Boolean {
        val children = this.children // this is a method call and it collects children
        return children.size > 1 && children[1] is KtTypeArgumentList
    }

    private fun LighterASTNode.hasAnyArguments(tree: FlyweightCapableTreeStructure<LighterASTNode>): Boolean {
        val children = getChildren(tree)
        return children.count { it != null } > 1 && children[1]?.tokenType == TYPE_ARGUMENT_LIST
    }

    private fun LighterASTNode.getChildren(tree: FlyweightCapableTreeStructure<LighterASTNode>): Array<out LighterASTNode?> {
        val childrenRef = Ref<Array<LighterASTNode>>()
        val childCount = tree.getChildren(this, childrenRef)
        return if (childCount > 0) childrenRef.get() else emptyArray()
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let {
            report(FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED.on(it))
        }
    }
}