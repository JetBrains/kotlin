/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.isInterface

object FirDelegationInInterfaceChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isInterface) {
            return
        }

        for (superTypeRef in declaration.superTypeRefs) {
            val source = superTypeRef.source ?: continue
            if (source.treeStructure.getParent(source.lighterASTNode)?.tokenType == KtNodeTypes.DELEGATED_SUPER_TYPE_ENTRY) {
                reporter.reportOn(source, FirErrors.DELEGATION_IN_INTERFACE, context)
            }
        }
    }
}
