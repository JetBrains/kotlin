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
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.lightNode
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry

object FirDelegationInInterfaceChecker : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirClass<*> || declaration.classKind != ClassKind.INTERFACE) {
            return
        }

        declaration.source?.findSuperTypeDelegation()?.let {
            reporter.report(declaration.superTypeRefs.getOrNull(it)?.source)
        }
    }

    private fun FirSourceElement.findSuperTypeDelegation(): Int {
        val localPsi = psi
        val localLightNode = lightNode

        if (localPsi != null && localPsi !is PsiErrorElement) {
            return localPsi.findSuperTypeDelegation()
        } else if (localLightNode != null && this is FirLightSourceElement) {
            return localLightNode.findSuperTypeDelegation(tree)
        }

        return -1
    }

    private fun PsiElement.findSuperTypeDelegation() = if (children.isNotEmpty() && children[0] !is PsiErrorElement) {
        children[0].children.indexOfFirst { it is KtDelegatedSuperTypeEntry }
    } else {
        -1
    }

    private fun LighterASTNode.findSuperTypeDelegation(tree: FlyweightCapableTreeStructure<LighterASTNode>): Int {
        val children = getChildren(tree)
        return if (children.isNotEmpty()) {
            children.find { it.tokenType == KtNodeTypes.SUPER_TYPE_LIST }
                ?.getChildren(tree)
                ?.indexOfFirst { it.tokenType == KtNodeTypes.DELEGATED_SUPER_TYPE_ENTRY }
                ?: -1
        } else {
            -1
        }
    }

    private fun LighterASTNode.getChildren(tree: FlyweightCapableTreeStructure<LighterASTNode>): List<LighterASTNode> {
        val children = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(this, children)
        return if (count > 0) children.get().filterNotNull() else emptyList()
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let { report(FirErrors.DELEGATION_IN_INTERFACE.on(it)) }
    }
}