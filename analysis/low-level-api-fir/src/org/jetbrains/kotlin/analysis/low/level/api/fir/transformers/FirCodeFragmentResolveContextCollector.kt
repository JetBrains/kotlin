/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.FirCodeFragmentContext
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirResolveContextCollector
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

internal class FirCodeFragmentResolveContextCollector(
    private val sessionHolder: SessionHolder,
    contextElement: KtElement
) : FirResolveContextCollector {
    private val contextElementCandidates = contextElement.parentsWithSelf.toSet()
    private val elementContexts = hashMapOf<KtElement, FirCodeFragmentContext>()

    val context: FirCodeFragmentContext
        get() = contextElementCandidates.firstNotNullOf { elementContexts[it] }

    override fun addFileContext(file: FirFile, context: FirTowerDataContext) {
        val psiFile = file.psi as? KtFile ?: return
        elementContexts[psiFile] = FileFirCodeFragmentContext(context)
    }

    override fun addStatementContext(statement: FirStatement, context: BodyResolveContext) {
        val element = statement.psi as? KtElement ?: return
        addContext(element, context)
    }

    override fun addDeclarationContext(declaration: FirDeclaration, context: BodyResolveContext) {
        val element = declaration.psi as? KtElement ?: return

        when (declaration) {
            is FirConstructor -> context.forConstructorBody(declaration, sessionHolder.session) {
                addContext(element, context)
            }
            is FirFunction -> context.forFunctionBody(declaration, sessionHolder) {
                addContext(element, context)
            }
            else -> addContext(element, context)
        }
    }

    private fun addContext(element: KtElement, context: BodyResolveContext) {
        if (element in contextElementCandidates) {
            elementContexts[element] = computeCodeFragmentContext(context)
        }
    }

    private fun computeCodeFragmentContext(context: BodyResolveContext): FirCodeFragmentContext {
        val towerDataContext = context.towerDataContext

        val variables = buildMap {
            val dataFlowAnalyzerContext = context.dataFlowAnalyzerContext
            val contextFlow = dataFlowAnalyzerContext.graphBuilder.lastNodeOrNull?.flow
            if (contextFlow != null) {
                for (realVariable in dataFlowAnalyzerContext.variableStorage.realVariables.values) {
                    val symbol = realVariable.identifier.symbol
                    val typeStatement = contextFlow.getTypeStatement(realVariable) ?: continue
                    put(symbol, typeStatement.exactType)
                }
            }
        }

        return StatementFirCodeFragmentContext(towerDataContext, variables)
    }
}

private class FileFirCodeFragmentContext(override val towerDataContext: FirTowerDataContext) : FirCodeFragmentContext {
    override val variables: Map<FirBasedSymbol<*>, Set<ConeKotlinType>>
        get() = emptyMap()
}

private class StatementFirCodeFragmentContext(
    override val towerDataContext: FirTowerDataContext,
    override val variables: Map<FirBasedSymbol<*>, Set<ConeKotlinType>>
) : FirCodeFragmentContext