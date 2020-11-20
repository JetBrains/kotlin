/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.modality
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(DfaInternals::class)
class VariableStorage(private val session: FirSession) {
    private var counter = 1
    private val realVariables: MutableMap<Identifier, RealVariable> = HashMap()
    private val syntheticVariables: MutableMap<FirElement, SyntheticVariable> = HashMap()

    fun clear(): VariableStorage = VariableStorage(session)

    fun getOrCreateRealVariableWithoutUnwrappingAlias(flow: Flow, symbol: AbstractFirBasedSymbol<*>, fir: FirElement): RealVariable {
        val realFir = fir.unwrapElement()
        val identifier = getIdentifierBySymbol(flow, symbol, realFir)
        return realVariables.getOrPut(identifier) { createRealVariableInternal(flow, identifier, realFir) }
    }

    private fun getOrCreateRealVariable(flow: Flow, symbol: AbstractFirBasedSymbol<*>, fir: FirElement): RealVariable {
        val variable = getOrCreateRealVariableWithoutUnwrappingAlias(flow, symbol, fir)
        return flow.directAliasMap[variable]?.variable ?: variable
    }

    private fun FirElement.unwrapElement(): FirElement = when (this) {
        is FirWhenSubjectExpression -> whenRef.value.let { it.subjectVariable ?: it.subject }?.unwrapElement() ?: this
        is FirExpressionWithSmartcast -> originalExpression.unwrapElement()
        is FirSafeCallExpression -> regularQualifiedAccess.unwrapElement()
        is FirCheckedSafeCallSubject -> originalReceiverRef.value.unwrapElement()
        else -> this
    }

    private fun getIdentifierBySymbol(
        flow: Flow,
        symbol: AbstractFirBasedSymbol<*>,
        fir: FirElement,
    ): Identifier {
        val expression = fir as? FirQualifiedAccess
        return Identifier(
            symbol,
            expression?.dispatchReceiver?.takeIf { it != FirNoReceiverExpression }?.let { getOrCreateVariable(flow, it) },
            expression?.extensionReceiver?.takeIf { it != FirNoReceiverExpression }?.let { getOrCreateVariable(flow, it) }
        )
    }

    /**
     * [originalFir] used for extracting expression under <when_subject> and extracting receiver
     */
    private fun createRealVariableInternal(flow: Flow, identifier: Identifier, originalFir: FirElement): RealVariable {
        val receiver: FirExpression?
        val isThisReference: Boolean
        val expression: FirQualifiedAccess? = when (originalFir) {
            is FirQualifiedAccessExpression -> originalFir
            is FirWhenSubjectExpression -> originalFir.whenRef.value.subject as? FirQualifiedAccessExpression
            is FirVariableAssignment -> originalFir
            else -> null
        }

        if (expression != null) {
            receiver = expression.explicitReceiver
            isThisReference = expression.calleeReference is FirThisReference
        } else {
            receiver = null
            isThisReference = false
        }

        val receiverVariable = receiver?.let { getOrCreateVariable(flow, it) }
        return RealVariable(identifier, isThisReference, receiverVariable, counter++)
    }

    @JvmName("getOrCreateRealVariableOrNull")
    fun getOrCreateRealVariable(flow: Flow, symbol: AbstractFirBasedSymbol<*>?, fir: FirElement): RealVariable? =
        symbol.takeIf { it.isStable(fir) }?.let { getOrCreateRealVariable(flow, it, fir) }

    fun createSyntheticVariable(fir: FirElement): SyntheticVariable =
        SyntheticVariable(fir, counter++).also { syntheticVariables[fir] = it }

    fun getOrCreateVariable(flow: Flow, fir: FirElement): DataFlowVariable {
        val realFir = fir.unwrapElement()
        val symbol = realFir.symbol
        return if (symbol.isStable(realFir)) {
            getOrCreateRealVariable(flow, symbol!!, realFir)
        } else {
            syntheticVariables[realFir] ?: createSyntheticVariable(realFir)
        }
    }

    fun getRealVariableWithoutUnwrappingAlias(symbol: AbstractFirBasedSymbol<*>?, fir: FirElement, flow: Flow): RealVariable? {
        val realFir = fir.unwrapElement()
        return symbol.takeIf { it.isStable(realFir) }?.let {
            realVariables[getIdentifierBySymbol(flow, it, realFir.unwrapElement())]
        }
    }

    fun getRealVariable(symbol: AbstractFirBasedSymbol<*>?, fir: FirElement, flow: Flow): RealVariable? {
        return getRealVariableWithoutUnwrappingAlias(symbol, fir, flow)?.let { flow.unwrapVariable(it) }
    }

    fun getSyntheticVariable(fir: FirElement): SyntheticVariable? {
        return syntheticVariables[fir.unwrapElement()]
    }

    fun getVariable(fir: FirElement, flow: Flow): DataFlowVariable? {
        val realFir = fir.unwrapElement()
        val symbol = realFir.symbol
        return if (symbol.isStable(fir)) {
            getRealVariable(symbol, realFir, flow)
        } else {
            getSyntheticVariable(fir)
        }
    }

    fun removeRealVariable(symbol: AbstractFirBasedSymbol<*>) {
        realVariables.remove(Identifier(symbol, null, null))
    }

    fun removeSyntheticVariable(variable: DataFlowVariable) {
        if (variable !is SyntheticVariable) return
        syntheticVariables.remove(variable.fir)
    }

    @OptIn(ExperimentalContracts::class)
    fun AbstractFirBasedSymbol<*>?.isStable(originalFir: FirElement): Boolean {
        contract {
            returns(true) implies(this@isStable != null)
        }
        when (this) {
            is FirFunctionSymbol<*>,
            is FirClassSymbol<*>,
            is FirBackingFieldSymbol -> return true
            null -> return false
        }
        if (originalFir is FirThisReceiverExpression) return true
        if (this !is FirVariableSymbol<*>) return false

        val property = this.fir as? FirProperty ?: return true

        return when {
            property.isLocal -> true
            property.isVar -> false
            property.receiverTypeRef != null -> false
            property.getter.let { it != null && it !is FirDefaultPropertyAccessor } -> false
            property.modality != Modality.FINAL -> {
                val dispatchReceiver = (originalFir.unwrapElement() as? FirQualifiedAccess)?.dispatchReceiver ?: return false
                val receiverType = dispatchReceiver.typeRef.coneTypeSafe<ConeClassLikeType>()?.fullyExpandedType(session) ?: return false
                val receiverSymbol = receiverType.lookupTag.toSymbol(session) ?: return false
                when (val receiverFir = receiverSymbol.fir) {
                    is org.jetbrains.kotlin.fir.declarations.FirAnonymousObject -> true
                    is org.jetbrains.kotlin.fir.declarations.FirRegularClass -> receiverFir.modality == Modality.FINAL
                    else -> throw IllegalStateException("Should not be here: $receiverFir")
                }

            }
            else -> true
        }
    }
}
