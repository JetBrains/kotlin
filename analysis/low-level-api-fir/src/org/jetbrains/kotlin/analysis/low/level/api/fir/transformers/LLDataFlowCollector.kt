/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowCollector
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class LLDataFlowCollector : DataFlowCollector {
    override fun symbolTypeUpdated(target: FirBasedSymbol<*>, types: Set<ConeKotlinType>, fir: FirElement, container: FirDeclaration) {
        val declarationFlow = container.dataFlow
            ?: LLDeclarationDataFlow().also { container.dataFlow = it }

        val elementFlow = LLElementDataFlow(target, types)
        declarationFlow.register(fir, elementFlow)
    }
}

internal class LLDeclarationDataFlow {
    private val types = HashMap<FirElement, LLElementDataFlow>()

    operator fun get(fir: FirElement): LLElementDataFlow? {
        return types[fir]
    }

    fun register(fir: FirElement, flow: LLElementDataFlow) {
        val previousFlow = types.putIfAbsent(fir, flow)

        if (previousFlow != null) {
            errorWithAttachment("Duplicate element data flow") {
                withEntry("oldFlow") { print(previousFlow) }
                withEntry("newFlow") { print(flow) }
            }
        }
    }
}

internal data class LLElementDataFlow(val symbol: FirBasedSymbol<*>, val types: Set<ConeKotlinType>)

private object DeclarationDataFlowKey : FirDeclarationDataKey()

internal var FirDeclaration.dataFlow: LLDeclarationDataFlow? by FirDeclarationDataRegistry.data(DeclarationDataFlowKey)