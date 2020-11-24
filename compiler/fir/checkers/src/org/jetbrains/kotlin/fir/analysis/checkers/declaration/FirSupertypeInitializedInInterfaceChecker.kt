/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration

object FirSupertypeInitializedInInterfaceChecker : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirClass<*> || declaration.classKind != ClassKind.INTERFACE) {
            return
        }

        for (superTypeRef in declaration.superTypeRefs) {
            val source = superTypeRef.source ?: continue
            if (source.treeStructure.getParent(source.lighterASTNode)?.tokenType == KtNodeTypes.CONSTRUCTOR_CALLEE) {
                reporter.report(source)
            }
        }
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let { report(FirErrors.SUPERTYPE_INITIALIZED_IN_INTERFACE.on(it)) }
    }
}