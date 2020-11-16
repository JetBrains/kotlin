/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry

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
        val localPsi = psi
        val localLightNode = lighterASTNode

        if (localPsi != null && localPsi !is PsiErrorElement) {
            return localPsi.anySupertypeHasConstructorParentheses()
        } else if (this is FirLightSourceElement) {
            return localLightNode.anySupertypeHasConstructorParentheses(tree)
        }

        return false
    }

    private fun PsiElement.anySupertypeHasConstructorParentheses(): Boolean {
        val children = this.children // this is a method call and it collects children
        return children.isNotEmpty() && children[0] !is PsiErrorElement && children[0].children.any { it is KtSuperTypeCallEntry }
    }

    private fun LighterASTNode.anySupertypeHasConstructorParentheses(tree: FlyweightCapableTreeStructure<LighterASTNode>): Boolean {
        val superTypes = getChildren(tree).find { it.tokenType == KtNodeTypes.SUPER_TYPE_LIST }
            ?: return false

        return superTypes.getChildren(tree).any { it.tokenType == KtNodeTypes.SUPER_TYPE_CALL_ENTRY }
    }

    private fun LighterASTNode.getChildren(tree: FlyweightCapableTreeStructure<LighterASTNode>): List<LighterASTNode> {
        val children = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(this, children)
        return if (count > 0) children.get().filterNotNull() else emptyList()
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let { report(FirErrors.SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR.on(it)) }
    }
}
