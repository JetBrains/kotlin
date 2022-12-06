/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import com.intellij.lang.LighterASTNode
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.name.FqName

object FirJvmDelegateToDefaultsChecker : FirAnnotationChecker() {
    private val JVM_DELEGATE_TO_DEFAULTS_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmDelegateToDefaults")

    override fun check(expression: FirAnnotation, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.fqName(context.session) == JVM_DELEGATE_TO_DEFAULTS_ANNOTATION_FQ_NAME) {
            val tree = expression.source?.treeStructure ?: return
            if (!belongsToDelegatedSuperTypeEntry(expression.source!!.lighterASTNode, tree))
            reporter.reportOn(expression.source, FirErrors.WRONG_ANNOTATION_TARGET, "expression", context)
        }
    }
}

private fun belongsToDelegatedSuperTypeEntry(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): Boolean {
    val parent = tree.getParent(node)
    return when (parent?.tokenType) {
        null -> false
        KtNodeTypes.DELEGATED_SUPER_TYPE_ENTRY -> true
        KtNodeTypes.ANNOTATED_EXPRESSION, KtNodeTypes.PARENTHESIZED, KtNodeTypes.LABELED_EXPRESSION -> belongsToDelegatedSuperTypeEntry(parent, tree)
        else -> false
    }
}