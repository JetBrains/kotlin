/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.originalOrSelf
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

@OptIn(DfaInternals::class)
class VariableStorageImpl(private val session: FirSession) : VariableStorage() {
    private var counter = 1
    private val _realVariables: MutableMap<Identifier, RealVariable> = HashMap()
    val realVariables: Map<Identifier, RealVariable>
        get() = _realVariables

    private val syntheticVariables: MutableMap<FirElement, SyntheticVariable> = HashMap()

    fun clear(): VariableStorageImpl = VariableStorageImpl(session)

    fun getOrCreateRealVariableWithoutUnwrappingAliasForPropertyInitialization(
        flow: Flow,
        symbol: FirBasedSymbol<*>,
        fir: FirElement,
    ): RealVariable {
        val realFir = fir.unwrapElement()
        val identifier = getIdentifierBySymbol(flow, symbol, realFir)
        val stability = symbol.getStability(realFir)
        requireWithAttachment(stability != null, { "Stability for initialized variable always should be computable" }) {
            withFirSymbolEntry("symbol", symbol)
            withFirEntry("fir", fir)
            withEntry("identifier", identifier.toString())
        }

        return _realVariables[identifier] ?: createReal(flow, identifier, realFir, stability)
    }

    override fun getRealVariableWithoutUnwrappingAlias(flow: Flow, fir: FirElement): RealVariable? {
        val realFir = fir.unwrapElement()
        val symbol = realFir.symbol ?: return null
        if (symbol.getStability(realFir) == null) return null
        return _realVariables[getIdentifierBySymbol(flow, symbol, realFir)]
    }

    override fun getLocalVariable(symbol: FirBasedSymbol<*>): RealVariable? =
        _realVariables[Identifier(symbol, null, null)]

    // General pattern when using these function:
    //
    //   val argumentVariable = variableStorage.{get,getOrCreateIfReal}(flow, fir.argument) ?: return
    //   val expressionVariable = variableStorage.createSynthetic(fir)
    //   flow.addImplication(somethingAbout(expressionVariable) implies somethingElseAbout(argumentVariable))
    //
    // If "something else" is a type/nullability statement, use `getOrCreateIfReal`; if it's `... == true/false`, use `get`.
    // The point is to only create variables and statements if they lead to useful conclusions; if a variable
    // does not exist, then no statements about it have been made, and if it's synthetic, none will be created later.
    override fun get(flow: Flow, fir: FirElement, unwrapAlias: Boolean): DataFlowVariable? {
        return get(flow, fir.unwrapElement(), createReal = false, unwrapAlias)
    }

    fun getOrCreateIfReal(flow: Flow, fir: FirElement, unwrapAlias: Boolean): DataFlowVariable? {
        return get(flow, fir.unwrapElement(), createReal = true, unwrapAlias)
    }

    fun getOrCreate(flow: Flow, fir: FirElement, unwrapAlias: Boolean): DataFlowVariable =
        fir.unwrapElement().let { get(flow, it, createReal = true, unwrapAlias) ?: createSynthetic(it) }

    fun createSynthetic(fir: FirElement): SyntheticVariable =
        SyntheticVariable(fir, counter++).also { syntheticVariables[fir] = it }

    private fun get(flow: Flow, realFir: FirElement, createReal: Boolean, unwrapAlias: Boolean): DataFlowVariable? {
        val symbol = realFir.symbol
        val stability = symbol?.getStability(realFir) ?: return syntheticVariables[realFir]
        val identifier = getIdentifierBySymbol(flow, symbol, realFir)
        val realVariable = _realVariables[identifier]
        if (realVariable != null) {
            if (unwrapAlias) return flow.unwrapVariable(realVariable)
            return realVariable
        }
        return if (createReal) createReal(flow, identifier, realFir, stability) else null
    }

    fun removeRealVariable(symbol: FirBasedSymbol<*>) {
        _realVariables.remove(Identifier(symbol, null, null))
    }

    private fun getIdentifierBySymbol(flow: Flow, symbol: FirBasedSymbol<*>, fir: FirElement): Identifier {
        val expression = fir as? FirQualifiedAccessExpression ?: (fir as? FirVariableAssignment)?.lValue as? FirQualifiedAccessExpression
        // TODO: don't create receiver variables if not going to create the composed variable either?
        return Identifier(
            symbol,
            expression?.dispatchReceiver?.let { getOrCreate(flow, it, unwrapAlias = true) },
            expression?.extensionReceiver?.let { getOrCreate(flow, it, unwrapAlias = true) }
        )
    }

    private fun createReal(flow: Flow, identifier: Identifier, originalFir: FirElement, stability: PropertyStability): RealVariable {
        val receiver: FirExpression?
        val isThisReference: Boolean
        val expression: FirQualifiedAccessExpression? = when (originalFir) {
            is FirQualifiedAccessExpression -> originalFir
            is FirWhenSubjectExpression -> originalFir.whenRef.value.subject as? FirQualifiedAccessExpression
            is FirVariableAssignment -> originalFir.unwrapLValue()
            else -> null
        }

        if (expression != null) {
            receiver = expression.explicitReceiver
            isThisReference = expression.calleeReference is FirThisReference
        } else {
            receiver = null
            isThisReference = false
        }

        val receiverVariable = receiver?.let { getOrCreate(flow, it, unwrapAlias = true) }
        return RealVariable(identifier, isThisReference, receiverVariable, counter++, stability).also {
            _realVariables[identifier] = it
        }
    }

    fun copyRealVariableWithRemapping(variable: RealVariable, from: RealVariable, to: RealVariable): RealVariable {
        val newIdentifier = with(variable.identifier) {
            copy(
                dispatchReceiver = if (dispatchReceiver == from) to else dispatchReceiver,
                extensionReceiver = if (extensionReceiver == from) to else extensionReceiver,
            )
        }
        return getOrPut(newIdentifier) {
            with(variable) {
                RealVariable(
                    newIdentifier, isThisReference, if (explicitReceiverVariable == from) to else explicitReceiverVariable,
                    counter++, stability
                )
            }
        }
    }

    fun getOrPut(identifier: Identifier, factory: () -> RealVariable): RealVariable {
        return _realVariables.getOrPut(identifier, factory)
    }

    private fun FirBasedSymbol<*>.getStability(realFir: FirElement): PropertyStability? {
        if (realFir is FirThisReceiverExpression) return PropertyStability.STABLE_VALUE
        when (this) {
            is FirAnonymousObjectSymbol -> return null
            is FirFunctionSymbol<*>, is FirClassSymbol<*> -> return PropertyStability.STABLE_VALUE
            is FirBackingFieldSymbol -> return if (isVal) PropertyStability.STABLE_VALUE else PropertyStability.MUTABLE_PROPERTY
        }
        if (this is FirCallableSymbol && this.isExpect) return PropertyStability.EXPECT_PROPERTY
        if (this !is FirVariableSymbol<*>) return null
        if (this is FirFieldSymbol && !this.isFinal) return PropertyStability.MUTABLE_PROPERTY

        val property = when (val variable = this.fir) { // intentionally exhaustive 'when'
            is FirEnumEntry, is FirErrorProperty, is FirValueParameter -> return PropertyStability.STABLE_VALUE

            // NB: FirJavaField is expected here. FirFieldImpl should've been handled by FirBackingFieldSymbol check above
            is FirField -> {
                if (variable.isJava)
                    return variable.determineStabilityByModule()
                else
                    errorWithAttachment("Expected to handle non-Java FirFields via symbol-based checks") {
                        withFirEntry("fir", variable)
                    }
            }

            is FirBackingField -> errorWithAttachment("Expected to handle Backing Field entirely via symbol-based checks") {
                withFirEntry("fir", variable)
            }

            is FirProperty -> variable
        }

        return when {
            property.delegate != null -> PropertyStability.DELEGATED_PROPERTY
            property.isLocal -> if (property.isVal) PropertyStability.STABLE_VALUE else PropertyStability.LOCAL_VAR
            property.isVar -> PropertyStability.MUTABLE_PROPERTY
            property.receiverParameter != null -> PropertyStability.PROPERTY_WITH_GETTER
            property.getter.let { it != null && it !is FirDefaultPropertyAccessor } -> PropertyStability.PROPERTY_WITH_GETTER
            property.visibility == Visibilities.Private -> PropertyStability.STABLE_VALUE
            property.modality != Modality.FINAL && !realFir.hasFinalDispatchReceiver() -> PropertyStability.PROPERTY_WITH_GETTER
            else -> property.determineStabilityByModule()
        }
    }

    private fun FirElement.hasFinalDispatchReceiver(): Boolean {
        val dispatchReceiver = (this as? FirQualifiedAccessExpression)?.dispatchReceiver ?: return false
        val receiverType = dispatchReceiver.resolvedType.lowerBoundIfFlexible().fullyExpandedType(session)
        val receiverFir = (receiverType as? ConeClassLikeType)?.lookupTag?.toSymbol(session)?.fir ?: return false
        return receiverFir is FirAnonymousObject || receiverFir.modality == Modality.FINAL
    }

    private fun FirVariable.determineStabilityByModule(): PropertyStability {
        val propertyModuleData = originalOrSelf().moduleData
        val currentModuleData = session.moduleData
        return when (propertyModuleData) {
            currentModuleData,
            in currentModuleData.friendDependencies,
            in currentModuleData.allDependsOnDependencies,
            -> PropertyStability.STABLE_VALUE
            else -> PropertyStability.ALIEN_PUBLIC_PROPERTY
        }
    }
}
