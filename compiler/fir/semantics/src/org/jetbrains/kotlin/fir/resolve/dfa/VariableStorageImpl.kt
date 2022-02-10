/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.originalOrSelf
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(DfaInternals::class)
class VariableStorageImpl(private val session: FirSession) : VariableStorage() {
    private var counter = 1
    private val _realVariables: MutableMap<Identifier, RealVariable> = HashMap()
    val realVariables: Map<Identifier, RealVariable>
        get() = _realVariables

    private val syntheticVariables: MutableMap<FirElement, SyntheticVariable> = HashMap()

    fun clear(): VariableStorageImpl = VariableStorageImpl(session)

    fun getOrCreateRealVariableWithoutUnwrappingAlias(
        flow: Flow,
        symbol: FirBasedSymbol<*>,
        fir: FirElement,
        stability: PropertyStability
    ): RealVariable {
        val realFir = fir.unwrapElement()
        val identifier = getIdentifierBySymbol(flow, symbol, realFir)
        return _realVariables.getOrPut(identifier) { createRealVariableInternal(flow, identifier, realFir, stability) }
    }

    private fun getOrCreateRealVariable(
        flow: Flow,
        symbol: FirBasedSymbol<*>,
        fir: FirElement,
        stability: PropertyStability
    ): RealVariable {
        val variable = getOrCreateRealVariableWithoutUnwrappingAlias(flow, symbol, fir, stability)
        return flow.directAliasMap[variable]?.variable ?: variable
    }

    private fun FirElement.unwrapElement(): FirElement = when (this) {
        is FirWhenSubjectExpression -> whenRef.value.let { it.subjectVariable ?: it.subject }?.unwrapElement() ?: this
        is FirExpressionWithSmartcast -> originalExpression.unwrapElement()
        is FirSafeCallExpression -> selector.unwrapElement()
        is FirCheckedSafeCallSubject -> originalReceiverRef.value.unwrapElement()
        is FirCheckNotNullCall -> argument.unwrapElement()
        else -> this
    }

    private fun getIdentifierBySymbol(
        flow: Flow,
        symbol: FirBasedSymbol<*>,
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
    private fun createRealVariableInternal(
        flow: Flow,
        identifier: Identifier,
        originalFir: FirElement,
        stability: PropertyStability
    ): RealVariable {
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
        return RealVariable(identifier, isThisReference, receiverVariable, counter++, stability).also {
            (receiverVariable as? RealVariable)?.dependentVariables?.add(it)
        }
    }

    @JvmName("getOrCreateRealVariableOrNull")
    fun getOrCreateRealVariable(flow: Flow, symbol: FirBasedSymbol<*>?, fir: FirElement): RealVariable? =
        symbol.getStability(fir)?.let { getOrCreateRealVariable(flow, symbol!!, fir, it) }

    fun createSyntheticVariable(fir: FirElement): SyntheticVariable =
        SyntheticVariable(fir, counter++).also { syntheticVariables[fir] = it }

    fun getOrCreateVariable(flow: Flow, fir: FirElement): DataFlowVariable {
        val realFir = fir.unwrapElement()
        val symbol = realFir.symbol
        val stability = symbol.getStability(realFir)
        return if (stability != null) {
            getOrCreateRealVariable(flow, symbol!!, realFir, stability)
        } else {
            syntheticVariables[realFir] ?: createSyntheticVariable(realFir)
        }
    }

    override fun getRealVariableWithoutUnwrappingAlias(flow: Flow, symbol: FirBasedSymbol<*>?, fir: FirElement): RealVariable? {
        val realFir = fir.unwrapElement()
        return symbol.takeIf { it.getStability(realFir) != null }?.let {
            _realVariables[getIdentifierBySymbol(flow, it, realFir.unwrapElement())]
        }
    }

    override fun getRealVariable(flow: Flow, symbol: FirBasedSymbol<*>?, fir: FirElement): RealVariable? {
        return getRealVariableWithoutUnwrappingAlias(flow, symbol, fir)?.let { flow.unwrapVariable(it) }
    }

    override fun getSyntheticVariable(fir: FirElement): SyntheticVariable? {
        return syntheticVariables[fir.unwrapElement()]
    }

    override fun getVariable(flow: Flow, fir: FirElement): DataFlowVariable? {
        val realFir = fir.unwrapElement()
        val symbol = realFir.symbol
        val stability = symbol.getStability(fir)
        return if (stability != null) {
            getRealVariable(flow, symbol, realFir)
        } else {
            getSyntheticVariable(fir)
        }
    }

    fun removeRealVariable(symbol: FirBasedSymbol<*>) {
        _realVariables.remove(Identifier(symbol, null, null))
    }

    fun removeSyntheticVariable(variable: DataFlowVariable) {
        if (!variable.isSynthetic()) return
        syntheticVariables.remove(variable.fir)
    }

    @OptIn(ExperimentalContracts::class)
    fun FirBasedSymbol<*>?.getStability(originalFir: FirElement): PropertyStability? {
        contract {
            returnsNotNull() implies (this@getStability != null)
        }
        when (this) {
            is FirAnonymousObjectSymbol -> return null
            is FirFunctionSymbol<*>,
            is FirClassSymbol<*>,
            is FirBackingFieldSymbol -> return PropertyStability.STABLE_VALUE
            null -> return null
        }
        if (originalFir is FirThisReceiverExpression) return PropertyStability.STABLE_VALUE
        if (this !is FirVariableSymbol<*>) return null
        if (this is FirFieldSymbol && !this.isFinal) return PropertyStability.MUTABLE_PROPERTY

        val property = this.fir as? FirProperty ?: return PropertyStability.STABLE_VALUE

        return when {
            property.delegate != null -> PropertyStability.DELEGATED_PROPERTY
            property.isLocal -> if (property.isVal) PropertyStability.STABLE_VALUE else PropertyStability.LOCAL_VAR
            property.isVar -> PropertyStability.MUTABLE_PROPERTY
            property.receiverTypeRef != null -> PropertyStability.PROPERTY_WITH_GETTER
            property.getter.let { it != null && it !is FirDefaultPropertyAccessor } -> PropertyStability.PROPERTY_WITH_GETTER
            property.modality != Modality.FINAL -> {
                val dispatchReceiver = (originalFir.unwrapElement() as? FirQualifiedAccess)?.dispatchReceiver ?: return null
                val receiverType = dispatchReceiver.typeRef.coneTypeSafe<ConeClassLikeType>()?.fullyExpandedType(session) ?: return null
                val receiverSymbol = receiverType.lookupTag.toSymbol(session) ?: return null
                when (val receiverFir = receiverSymbol.fir) {
                    is FirAnonymousObject -> PropertyStability.STABLE_VALUE
                    is FirRegularClass -> if (receiverFir.modality == Modality.FINAL) PropertyStability.STABLE_VALUE else PropertyStability.PROPERTY_WITH_GETTER
                    else -> throw IllegalStateException("Should not be here: $receiverFir")
                }
            }
            else -> {
                val propertyModuleData = property.originalOrSelf().moduleData
                val currentModuleData = session.moduleData
                when (propertyModuleData) {
                    currentModuleData, in currentModuleData.dependsOnDependencies -> PropertyStability.STABLE_VALUE
                    else -> PropertyStability.ALIEN_PUBLIC_PROPERTY
                }
            }
        }
    }
}
