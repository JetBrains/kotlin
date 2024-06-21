/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker
import org.jetbrains.kotlin.fir.analysis.cfa.util.*
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isIterator
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.isCatchParameter
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.SpecialNames

object UnusedChecker : AbstractFirPropertyInitializationChecker(MppCheckerKind.Common) {
    override fun analyze(data: VariableInitializationInfoData, reporter: DiagnosticReporter, context: CheckerContext) {
        @Suppress("UNCHECKED_CAST")
        val properties = data.properties as Set<FirPropertySymbol>
        val ownData = Data(properties)
        data.graph.traverse(AddAllWrites(), ownData)
        if (ownData.unreadWrites.isNotEmpty()) {
            ownData.writesByNode = data.graph.traverseToFixedPoint(FindVisibleWrites(properties))
        }
        data.graph.traverse(RemoveVisibleWrites(), ownData)

        val variablesWithUnobservedWrites = mutableSetOf<FirPropertySymbol>()
        for (statement in ownData.unreadWrites) {
            if (statement is FirVariableAssignment) {
                val variableSymbol = statement.calleeReference?.toResolvedPropertySymbol() ?: continue
                variablesWithUnobservedWrites.add(variableSymbol)
                // TODO, KT-59831: report case like "a += 1" where `a` `doesn't writes` different way (special for Idea)
                reporter.reportOn(statement.lValue.source, FirErrors.ASSIGNED_VALUE_IS_NEVER_READ, context)
            } else if (statement is FirProperty) {
                if (statement.symbol !in ownData.variablesWithoutReads && !statement.symbol.ignoreWarnings) {
                    reporter.reportOn(statement.initializer?.source, FirErrors.VARIABLE_INITIALIZER_IS_REDUNDANT, context)
                }
            }
        }

        for ((symbol, fir) in ownData.variablesWithoutReads) {
            if (symbol.ignoreWarnings) continue
            if ((fir.initializer as? FirFunctionCall)?.isIterator == true || fir.isCatchParameter == true) continue
            val error = if (symbol in variablesWithUnobservedWrites) FirErrors.VARIABLE_NEVER_READ else FirErrors.UNUSED_VARIABLE
            reporter.reportOn(fir.source, error, context)
        }
    }

    private val FirPropertySymbol.ignoreWarnings: Boolean
        get() = name == SpecialNames.UNDERSCORE_FOR_UNUSED_VAR ||
                source == null ||
                // if <receiver> variable is reported as unused,
                // then the assignment itself is a dead code because of its RHS expression,
                // which will be eventually reported
                source?.kind is KtFakeSourceElementKind.DesugaredAugmentedAssign ||
                source?.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION ||
                initializerSource?.kind == KtFakeSourceElementKind.DesugaredForLoop

    private class Data(val localProperties: Set<FirPropertySymbol>) {
        var writesByNode: Map<CFGNode<*>, PathAwareVariableWriteInfo> = emptyMap()
        val unreadWrites: MutableSet<FirStatement /* FirProperty | FirVariableAssignment */> = mutableSetOf()
        val variablesWithoutReads: MutableMap<FirPropertySymbol, FirProperty> = mutableMapOf()
    }

    private class AddAllWrites : ControlFlowGraphVisitor<Unit, Data>() {
        override fun visitNode(node: CFGNode<*>, data: Data) {}

        override fun visitVariableDeclarationNode(node: VariableDeclarationNode, data: Data) {
            if (node.fir.initializer != null) {
                data.unreadWrites.add(node.fir)
            }
            data.variablesWithoutReads[node.fir.symbol] = node.fir
        }

        override fun visitVariableAssignmentNode(node: VariableAssignmentNode, data: Data) {
            if (node.fir.calleeReference?.toResolvedPropertySymbol() in data.localProperties) {
                data.unreadWrites.add(node.fir)
            }
        }
    }

    private class FindVisibleWrites(private val properties: Set<FirPropertySymbol>) :
        PathAwareControlFlowGraphVisitor<FirPropertySymbol, VariableWrites>() {

        override fun mergeInfo(a: VariableWriteInfo, b: VariableWriteInfo, node: CFGNode<*>): VariableWriteInfo =
            a.merge(b, VariableWrites::addAll)

        private fun PathAwareVariableWriteInfo.overwrite(symbol: FirPropertySymbol, data: VariableWrites): PathAwareVariableWriteInfo =
            transformValues { it.put(symbol, data) }

        override fun visitVariableDeclarationNode(
            node: VariableDeclarationNode,
            data: PathAwareVariableWriteInfo,
        ): PathAwareVariableWriteInfo =
            data.overwrite(node.fir.symbol, if (node.fir.initializer != null) persistentSetOf(node) else persistentSetOf())

        override fun visitVariableAssignmentNode(
            node: VariableAssignmentNode,
            data: PathAwareVariableWriteInfo,
        ): PathAwareVariableWriteInfo {
            val symbol = node.fir.calleeReference?.toResolvedPropertySymbol()?.takeIf { it in properties } ?: return data
            return data.overwrite(symbol, persistentSetOf(node))
        }
    }

    private class RemoveVisibleWrites : ControlFlowGraphVisitor<Unit, Data>() {
        override fun visitNode(node: CFGNode<*>, data: Data) {
            visitAnnotations(node, data)
        }

        override fun visitQualifiedAccessNode(node: QualifiedAccessNode, data: Data) {
            visitAnnotations(node, data)
            visitQualifiedAccess(node, node.fir, data)
        }

        override fun visitFunctionCallExitNode(node: FunctionCallExitNode, data: Data) {
            visitAnnotations(node, data)
            // TODO, KT-64094: receiver of implicit invoke calls does not generate a QualifiedAccessNode.
            ((node.fir as? FirImplicitInvokeCall)?.explicitReceiver as? FirQualifiedAccessExpression)
                ?.let { visitQualifiedAccess(node, it, data) }
        }

        private fun visitAnnotations(node: CFGNode<*>, data: Data) {
            // Annotations don't create CFG nodes. Note that annotations can only refer to `const val`s, so any read
            // of a local inside an annotation is inherently incorrect. Still, use them to suppress the warnings.
            (node.fir as? FirAnnotationContainer)?.annotations?.forEach { call ->
                call.accept(object : FirVisitorVoid() {
                    override fun visitElement(element: FirElement) {
                        if (element is FirQualifiedAccessExpression) {
                            visitQualifiedAccess(node, element, data)
                        }
                        element.acceptChildren(this)
                    }
                })
            }
        }

        private fun visitQualifiedAccess(node: CFGNode<*>, fir: FirQualifiedAccessExpression, data: Data) {
            val symbol = fir.calleeReference.toResolvedPropertySymbol() ?: return
            data.variablesWithoutReads.remove(symbol)
            data.writesByNode[node]?.values?.forEach { dataForLabel ->
                dataForLabel[symbol]?.forEach { data.unreadWrites.remove(it.fir) }
            }
        }
    }
}

private typealias VariableWrites = PersistentSet<CFGNode<*>>
private typealias VariableWriteInfo = ControlFlowInfo<FirPropertySymbol, VariableWrites>
private typealias PathAwareVariableWriteInfo = PathAwareControlFlowInfo<FirPropertySymbol, VariableWrites>
