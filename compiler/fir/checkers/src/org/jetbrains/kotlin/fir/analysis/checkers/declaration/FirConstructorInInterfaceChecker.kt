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
import org.jetbrains.kotlin.KtNodeTypes.PRIMARY_CONSTRUCTOR
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.KtPrimaryConstructor

object FirConstructorInInterfaceChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirClass<*> || declaration.classKind != ClassKind.INTERFACE) {
            return
        }

        if (declaration.source?.hasPrimaryConstructor() == true) {
            reporter.report(declaration.source)
        }
    }

    private fun FirSourceElement.hasPrimaryConstructor(): Boolean {
        val localPsi = psi
        val localLightNode = lighterASTNode

        if (localPsi != null && localPsi !is PsiErrorElement) {
            return localPsi.hasPrimaryConstructor()
        } else if (this is FirLightSourceElement) {
            return localLightNode.hasPrimaryConstructor(tree)
        }

        return false
    }

    private fun PsiElement.hasPrimaryConstructor(): Boolean {
        return lastChild !is PsiErrorElement && lastChild is KtPrimaryConstructor
    }

    private fun LighterASTNode.hasPrimaryConstructor(tree: FlyweightCapableTreeStructure<LighterASTNode>): Boolean {
        val children = getChildren(tree)
        return children.lastOrNull()?.tokenType == PRIMARY_CONSTRUCTOR
    }

    private fun LighterASTNode.getChildren(tree: FlyweightCapableTreeStructure<LighterASTNode>): List<LighterASTNode> {
        val children = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(this, children)
        return if (count > 0) children.get().filterNotNull() else emptyList()
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let { report(FirErrors.CONSTRUCTOR_IN_INTERFACE.on(it)) }
    }
}