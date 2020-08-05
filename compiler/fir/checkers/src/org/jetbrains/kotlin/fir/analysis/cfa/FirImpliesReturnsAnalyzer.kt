/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.contracts.collectReturnValueConditionalTypes
import org.jetbrains.kotlin.fir.contracts.coneEffects
import org.jetbrains.kotlin.fir.contracts.description.ConeConditionalEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeParametersEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.JumpNode
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker

object FirImpliesReturnsAnalyzer : FirControlFlowChecker() {

    override fun analyze(graph: ControlFlowGraph, reporter: DiagnosticReporter, checkerContext: CheckerContext) {
        val function = (graph.declaration as? FirFunction<*>) ?: return
        if (function.returnTypeRef.isUnit) return
        val effects = (function as? FirContractDescriptionOwner)?.contractDescription?.coneEffects ?: return

        val conditionalEffects =
            effects.filterIsInstance<ConeConditionalEffectDeclaration>().filter { it.effect is ConeParametersEffectDeclaration }
        if (conditionalEffects.isEmpty()) return


        for (effectDeclaration in conditionalEffects) {
            val conditionalTypes = effectDeclaration.collectReturnValueConditionalTypes(mutableListOf(), function.session.builtinTypes)
            if (conditionalTypes.isEmpty()) continue
            conditionalTypes.add(function.returnTypeRef.coneType)
            val conditionalType = ConeTypeIntersector.intersectTypes(function.session.typeContext, conditionalTypes)

            val wrongCondition = graph.exitNode.previousCfgNodes.any {
                isWrongConditionOnNode(it, conditionalType, effectDeclaration, function)
            }
        }
    }

    private fun isWrongConditionOnNode(
        node: CFGNode<*>,
        conditionalType: ConeKotlinType,
        effectDeclaration: ConeConditionalEffectDeclaration,
        function: FirFunction<*>
    ): Boolean {
        val effect = effectDeclaration.effect as ConeParametersEffectDeclaration

        val isReturn = node is JumpNode && node.fir is FirReturnExpression
        val resultExpression = if (isReturn) (node.fir as FirReturnExpression).result else node.fir
        val expressionTypeRef = (resultExpression as? FirExpression)?.typeRef ?: function.session.builtinTypes.nullableAnyType
        if (expressionTypeRef.isNothing) return false

        return !AbstractTypeChecker.isSubtypeOf(function.session.typeContext, expressionTypeRef.coneType, conditionalType)
    }
}