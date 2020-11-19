/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import com.intellij.lang.LighterASTNode
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.findChildByType
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass

object FirSupertypeInitializedWithoutPrimaryConstructor : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirRegularClass || declaration.classKind == ClassKind.INTERFACE) {
            return
        }

        val hasSupertypeWithConstructor = declaration.source?.anySupertypeHasConstructorParentheses() == true
        val hasPrimaryConstructor = declaration.declarations.any { it is FirConstructor && it.isPrimary }

        if (hasSupertypeWithConstructor && !hasPrimaryConstructor) {
            reporter.report(declaration.source)
        }
    }

    private fun FirSourceElement.anySupertypeHasConstructorParentheses(): Boolean {
        return lighterASTNode.anySupertypeHasConstructorParentheses(treeStructure)
    }

    private fun LighterASTNode.anySupertypeHasConstructorParentheses(tree: FlyweightCapableTreeStructure<LighterASTNode>): Boolean {
        val superTypes = tree.findChildByType(this, KtNodeTypes.SUPER_TYPE_LIST) ?: return false
        return tree.findChildByType(superTypes, KtNodeTypes.SUPER_TYPE_CALL_ENTRY) != null
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let { report(FirErrors.SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR.on(it)) }
    }
}
