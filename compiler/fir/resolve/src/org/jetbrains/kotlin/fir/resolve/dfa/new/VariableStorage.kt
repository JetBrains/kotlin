/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.new

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.impl.FirExplicitThisReference
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

class VariableStorage {
    private var counter = 1
    private val realVariables: MutableMap<Identifier, RealVariable> = HashMap()
    private val syntheticVariables: MutableMap<FirElement, SyntheticVariable> = HashMap()
    private val localVariableAliases: MutableMap<AbstractFirBasedSymbol<*>, Identifier> = HashMap()

    fun getOrCreateRealVariable(symbol: AbstractFirBasedSymbol<*>, fir: FirElement): RealVariable {
        val realFir = fir.unwrapElement()
        val identifier = getIdentifierBySymbol(symbol, realFir)
        return realVariables.getOrPut(identifier) { createRealVariableInternal(identifier, realFir) }
    }

    private fun FirElement.unwrapElement(): FirElement = when (this) {
        is FirWhenSubjectExpression -> whenSubject.whenExpression.let { it.subjectVariable ?: it.subject } ?: this
        is FirExpressionWithSmartcast -> originalExpression.unwrapElement()
        else -> this
    }

    private fun getIdentifierBySymbol(
        symbol: AbstractFirBasedSymbol<*>,
        fir: FirElement,
    ): Identifier {
        return localVariableAliases[symbol] ?: run {
            val expression = fir as? FirQualifiedAccessExpression
            Identifier(
                symbol,
                expression?.dispatchReceiver?.takeIf { it != FirNoReceiverExpression }?.let(this::getOrCreateVariable),
                expression?.extensionReceiver?.takeIf { it != FirNoReceiverExpression }?.let(this::getOrCreateVariable)
            )
        }
    }

    /**
     * [originalFir] used for extracting expression under <when_subject> and extracting receiver
     */
    private fun createRealVariableInternal(identifier: Identifier, originalFir: FirElement): RealVariable {
        val receiver: FirExpression?
        val isSafeCall: Boolean
        val isThisReference: Boolean
        val expression = when (originalFir) {
            is FirQualifiedAccessExpression -> originalFir
            is FirWhenSubjectExpression -> originalFir.whenSubject.whenExpression.subject as? FirQualifiedAccessExpression
            else -> null
        }

        if (expression != null) {
            receiver = expression.explicitReceiver
            isSafeCall = expression.safe
            isThisReference = expression.calleeReference is FirThisReference
        } else {
            receiver = null
            isSafeCall = false
            isThisReference = false
        }

        val receiverVariable = receiver?.let { getOrCreateVariable(it) }
        return RealVariable(identifier, isThisReference, receiverVariable, isSafeCall, counter++)
    }

    @JvmName("getOrCreateRealVariableOrNull")
    fun getOrCreateRealVariable(symbol: AbstractFirBasedSymbol<*>?, fir: FirElement): RealVariable? =
        symbol?.let { getOrCreateRealVariable(it, fir) }

    fun createSyntheticVariable(fir: FirElement): SyntheticVariable =
        SyntheticVariable(fir, counter++).also { syntheticVariables[fir] = it }

    fun getOrCreateVariable(fir: FirElement): DataFlowVariable {
        val realFir = fir.unwrapElement()
        return when (val symbol = realFir.symbol) {
            null -> syntheticVariables[realFir] ?: createSyntheticVariable(realFir)
            else -> getOrCreateRealVariable(symbol, realFir)
        }
    }

    /**
     * Also removes existing real variable for [varSymbol] if it exists
     */
    fun attachSymbolToVariable(varSymbol: AbstractFirBasedSymbol<*>, targetVariable: RealVariable) {
        localVariableAliases[varSymbol] = targetVariable.identifier
        realVariables.remove(Identifier(varSymbol, null, null))
    }

    operator fun get(symbol: AbstractFirBasedSymbol<*>?, fir: FirElement): RealVariable? {
        return symbol?.let { realVariables[getIdentifierBySymbol(it, fir.unwrapElement())] }
    }

    operator fun get(fir: FirElement): DataFlowVariable? {
        val realFir = fir.unwrapElement()
        val symbol = realFir.symbol
        return if (symbol != null) {
            get(symbol, realFir)
        } else {
            syntheticVariables[realFir]
        }
    }

    fun removeRealVariable(symbol: AbstractFirBasedSymbol<*>) {
        // TODO: this shit fails
//        assert(!localVariableAliases.containsValue(symbol))
        realVariables.remove(Identifier(symbol, null, null))
    }

    fun unboundPossiblyAliasedVariable(symbol: AbstractFirBasedSymbol<*>) {
        localVariableAliases.remove(symbol)
    }

    fun removeSyntheticVariable(variable: DataFlowVariable) {
        if (variable !is SyntheticVariable) return
        syntheticVariables.remove(variable.fir)
    }

    fun reset() {
        counter = 0
        realVariables.clear()
        syntheticVariables.clear()
        localVariableAliases.clear()
    }
}

internal val FirElement.symbol: AbstractFirBasedSymbol<*>?
    get() = when (this) {
        is FirResolvable -> symbol
        is FirSymbolOwner<*> -> symbol
        is FirWhenSubjectExpression -> whenSubject.whenExpression.subject?.symbol
        else -> null
    }?.takeIf { this is FirThisReceiverExpression || it !is FirFunctionSymbol<*> }

internal val FirResolvable.symbol: AbstractFirBasedSymbol<*>?
    get() = when (val reference = calleeReference) {
        is FirExplicitThisReference -> reference.boundSymbol
        is FirResolvedNamedReference -> reference.resolvedSymbol
        is FirNamedReferenceWithCandidate -> reference.candidateSymbol
        else -> null
    }