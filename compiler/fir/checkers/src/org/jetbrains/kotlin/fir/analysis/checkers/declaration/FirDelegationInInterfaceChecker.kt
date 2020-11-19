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
import org.jetbrains.kotlin.fir.analysis.checkers.getChildren
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration

object FirDelegationInInterfaceChecker : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirClass<*> || declaration.classKind != ClassKind.INTERFACE) {
            return
        }

        declaration.source?.findSuperTypeDelegation()?.let {
            reporter.report(declaration.superTypeRefs.getOrNull(it)?.source)
        }
    }

    private fun FirSourceElement.findSuperTypeDelegation(): Int =
        lighterASTNode.findSuperTypeDelegation(treeStructure)

    private fun LighterASTNode.findSuperTypeDelegation(tree: FlyweightCapableTreeStructure<LighterASTNode>): Int {
        val children = getChildren(tree)
        return if (children.isNotEmpty()) {
            children.find { it?.tokenType == KtNodeTypes.SUPER_TYPE_LIST }
                ?.getChildren(tree)
                ?.indexOfFirst { it?.tokenType == KtNodeTypes.DELEGATED_SUPER_TYPE_ENTRY }
                ?: -1
        } else {
            -1
        }
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let { report(FirErrors.DELEGATION_IN_INTERFACE.on(it)) }
    }
}