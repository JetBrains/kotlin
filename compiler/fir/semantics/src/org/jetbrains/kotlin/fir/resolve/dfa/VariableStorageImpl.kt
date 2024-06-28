/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.unwrapFakeOverrides

@OptIn(DfaInternals::class)
class VariableStorageImpl(private val session: FirSession) : VariableStorage() {
    private var counter = 1
    private val _realVariables: MutableMap<Identifier, RealVariable> = HashMap()
    val realVariables: Map<Identifier, RealVariable>
        get() = _realVariables

    private val syntheticVariables: MutableMap<FirElement, SyntheticVariable> = HashMap()

    fun clear(): VariableStorageImpl = VariableStorageImpl(session)

    fun getOrCreateRealVariableWithoutUnwrappingAliasForPropertyInitialization(
        symbol: FirBasedSymbol<*>,
        fir: FirElement,
        unwrap: (RealVariable, FirElement) -> RealVariable?,
    ): RealVariable? {
        val realFir = fir.unwrapElement()
        val identifier = getIdentifierBySymbol(symbol, realFir, unwrap) ?: return null
        return _realVariables[identifier] ?: createReal(identifier, realFir, unwrap)
    }

    override fun getRealVariableWithoutUnwrappingAlias(
        fir: FirElement,
        unwrapAlias: (RealVariable, FirElement) -> RealVariable?,
    ): RealVariable? {
        val realFir = fir.unwrapElement()
        val symbol = realFir.extractSymbol() ?: return null
        val identifier = getIdentifierBySymbol(symbol, realFir, unwrapAlias) ?: return null
        return _realVariables[identifier]
    }

    override fun getLocalVariable(symbol: FirBasedSymbol<*>): RealVariable? =
        _realVariables[Identifier(symbol, null, null)]

    // General pattern when using these function:
    //
    //   val argumentVariable = variableStorage.{get,getOrCreateIfReal}(fir.argument, unwrapAlias = { variable, element -> flow.unwrapVariable(it) }) ?: return
    //   val expressionVariable = variableStorage.createSynthetic(fir)
    //   flow.addImplication(somethingAbout(expressionVariable) implies somethingElseAbout(argumentVariable))
    //
    // If "something else" is a type/nullability statement, use `getOrCreateIfReal`; if it's `... == true/false`, use `get`.
    // The point is to only create variables and statements if they lead to useful conclusions; if a variable
    // does not exist, then no statements about it have been made, and if it's synthetic, none will be created later.

    /**
     * Get an existing [DataFlowVariable] for the specified [fir] [FirElement].
     *
     * @param unwrapAlias lambda used to transform a [RealVariable] if it represents an alias for another [RealVariable], or return the same
     * variable. If the alias is unstable, `null` can be returned. This will cause the function to also return `null`.
     */
    override fun get(fir: FirElement, unwrapAlias: (RealVariable, FirElement) -> RealVariable?): DataFlowVariable? {
        return get(fir.unwrapElement(), createReal = false, createSynthetic = false, unwrapAlias)
    }

    /**
     * Get an existing [DataFlowVariable], or create a [RealVariable] for the specified [fir] [FirElement] if possible.
     * If the variable does not already exist and cannot be represented by a [RealVariable], the function will return `null`.
     *
     * @param unwrapAlias lambda used to transform a [RealVariable] if it represents an alias for another [RealVariable], or return the same
     * variable. If the alias is unstable, `null` can be returned. This will cause the function to also return `null`.
     */
    fun getOrCreateIfReal(fir: FirElement, unwrapAlias: (RealVariable, FirElement) -> RealVariable?): DataFlowVariable? {
        return get(fir.unwrapElement(), createReal = true, createSynthetic = false, unwrapAlias)
    }

    /**
     * Get an existing [DataFlowVariable], or create a [DataFlowVariable] for the specified [fir] [FirElement].
     *
     * @param unwrapAlias lambda used to transform a [RealVariable] if it represents an alias for another [RealVariable], or return the same
     * variable. If the alias is unstable, `null` can be returned. This will cause the function to also return `null`.
     */
    fun getOrCreate(fir: FirElement, unwrapAlias: (RealVariable, FirElement) -> RealVariable?): DataFlowVariable? {
        return get(fir.unwrapElement(), createReal = true, createSynthetic = true, unwrapAlias)
    }

    fun createSynthetic(fir: FirElement): SyntheticVariable =
        SyntheticVariable(fir, counter++).also { syntheticVariables[fir] = it }

    private fun get(
        realFir: FirElement,
        createReal: Boolean,
        createSynthetic: Boolean,
        unwrapAlias: (RealVariable, FirElement) -> RealVariable?,
    ): DataFlowVariable? {
        val symbol = realFir.extractSymbol()
        if (symbol == null) {
            val syntheticVariable = syntheticVariables[realFir]
            return when {
                syntheticVariable != null -> syntheticVariable
                createSynthetic -> createSynthetic(realFir)
                else -> null
            }
        }

        val identifier = getIdentifierBySymbol(symbol, realFir, unwrapAlias) ?: return null
        val realVariable = _realVariables[identifier]
        return when {
            realVariable != null -> unwrapAlias(realVariable, realFir)
            createReal -> createReal(identifier, realFir, unwrapAlias)
            else -> null
        }
    }

    fun removeRealVariable(symbol: FirBasedSymbol<*>) {
        _realVariables.remove(Identifier(symbol, null, null))
    }

    private fun getIdentifierBySymbol(
        symbol: FirBasedSymbol<*>,
        fir: FirElement,
        unwrapAlias: (RealVariable, FirElement) -> RealVariable?,
    ): Identifier? {
        val expression = fir as? FirQualifiedAccessExpression ?: (fir as? FirVariableAssignment)?.lValue as? FirQualifiedAccessExpression

        // TODO: don't create receiver variables if not going to create the composed variable either?
        val dispatchReceiver = expression?.dispatchReceiver?.let { getOrCreate(it, unwrapAlias) ?: return null }
        val extensionReceiver = expression?.extensionReceiver?.let { getOrCreate(it, unwrapAlias) ?: return null }
        return Identifier(symbol, dispatchReceiver, extensionReceiver)
    }

    private fun createReal(
        identifier: Identifier,
        originalFir: FirElement,
        unwrapAlias: (RealVariable, FirElement) -> RealVariable?,
    ): RealVariable? {
        val expression = when (originalFir) {
            is FirExpression -> originalFir
            is FirVariableAssignment -> originalFir.unwrapLValue()
            else -> null
        }

        val isThisReference = expression is FirThisReceiverExpression
        val originalType = expression?.resolvedType ?: (originalFir as? FirProperty)?.returnTypeRef?.coneType
        val receiver = (expression as? FirQualifiedAccessExpression)?.explicitReceiver
        val receiverVariable = receiver?.let { getOrCreate(it, unwrapAlias) ?: return null }
        return RealVariable(identifier, originalType, isThisReference, receiverVariable, counter++).also {
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
                    newIdentifier, originalType, isThisReference, if (explicitReceiverVariable == from) to else explicitReceiverVariable,
                    counter++
                )
            }
        }
    }

    fun getOrPut(identifier: Identifier, factory: () -> RealVariable): RealVariable {
        return _realVariables.getOrPut(identifier, factory)
    }

    private fun FirElement.extractSymbol(): FirBasedSymbol<*>? = when (this) {
        is FirResolvable -> calleeReference.symbol.unwrapFakeOverridesIfNecessary()
        is FirVariableAssignment -> unwrapLValue()?.calleeReference?.symbol
        is FirDeclaration -> symbol.unwrapFakeOverridesIfNecessary()
        is FirWhenSubjectExpression -> whenRef.value.subject?.extractSymbol()
        is FirSafeCallExpression -> selector.extractSymbol()
        is FirSmartCastExpression -> originalExpression.extractSymbol()
        is FirDesugaredAssignmentValueReferenceExpression -> expressionRef.value.extractSymbol()
        is FirResolvedQualifier -> {
            fun symbolIfObject(symbol: FirClassifierSymbol<*>?): FirClassifierSymbol<*>? {
                return when (symbol) {
                    is FirRegularClassSymbol -> symbol.takeIf { it.classKind == ClassKind.OBJECT }
                    is FirTypeAliasSymbol -> symbolIfObject(symbol.fullyExpandedClass(session))
                    else -> null
                }
            }

            symbolIfObject(symbol)
        }
        else -> null
    }?.takeIf {
        this is FirThisReceiverExpression || it is FirClassSymbol || (it is FirVariableSymbol && it !is FirSyntheticPropertySymbol)
    }

    private fun FirBasedSymbol<*>?.unwrapFakeOverridesIfNecessary(): FirBasedSymbol<*>? {
        if (this !is FirCallableSymbol) return this
        // This is necessary only for sake of optimizations necessary because this is a really hot place.
        // Not having `dispatchReceiverType` means that this is a local/top-level property that can't be a fake override.
        // And at the same time, checking a field is much faster than a couple of attributes (0.3 secs at MT Full Kotlin)
        if (this.dispatchReceiverType == null) return this

        return this.unwrapFakeOverrides()
    }
}
