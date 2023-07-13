/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.BlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNothing
import org.jetbrains.kotlin.fir.types.isUnit

object FirFunctionReturnChecker : FirFunctionChecker() {

    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        checkHasReturnIfBlock(declaration, reporter, context)
    }

    private fun checkHasReturnIfBlock(
        declaration: FirFunction,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        if (declaration is FirPropertyAccessor && declaration.isSetter) return
        if (declaration is FirConstructor) return
        if (declaration is FirAnonymousFunction && declaration.isLambda) return
        val returnType = declaration.returnTypeRef.coneType.fullyExpandedType(context.session)
        if (returnType.isUnit || returnType.isNothing) return
        val graph = declaration.controlFlowGraphReference?.controlFlowGraph ?: return

        val blockExitNode = graph.exitNode.previousNodes.lastOrNull { it is BlockExitNode } ?: return
        if (!blockExitNode.isDead) {
            reporter.reportOn(declaration.source, FirErrors.NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY, context)
        }
    }
}