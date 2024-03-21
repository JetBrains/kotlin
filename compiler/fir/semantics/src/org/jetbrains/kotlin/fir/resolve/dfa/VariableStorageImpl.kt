/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

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
    private val realVariables: MutableMap<Identifier, RealVariable> = HashMap()
    private val syntheticVariables: MutableMap<FirElement, SyntheticVariable> = HashMap()

    fun clear(): VariableStorageImpl = VariableStorageImpl(session)

    private val nextVariableIndex: Int
        get() = realVariables.size + syntheticVariables.size + 1

    override fun getLocalVariable(symbol: FirBasedSymbol<*>, isReceiver: Boolean): RealVariable? =
        realVariables[Identifier(symbol, isReceiver, null, null)]

    fun getAllLocalVariables(): List<RealVariable> =
        realVariables.values.filter { (it.identifier.symbol as? FirPropertySymbol)?.isLocal == true }

    // Use this when making non-type statements, such as `variable eq true`.
    // Returns null if the statement would be useless (the variable has not been used in any implications).
    override fun getIfUsed(fir: FirElement, unwrapAlias: (RealVariable, FirElement) -> RealVariable?): DataFlowVariable? =
        getImpl(fir, createReal = false, unwrapAlias)?.takeSyntheticIfKnown()

    // Use this when making type statements, such as `variable typeEq ...` or `variable notEq null`.
    // Returns null if the statement would be useless (the variable is synthetic and has not been used in any implications).
    fun getOrCreateIfReal(fir: FirElement, unwrapAlias: (RealVariable, FirElement) -> RealVariable?): DataFlowVariable? =
        getImpl(fir, createReal = true, unwrapAlias)?.takeSyntheticIfKnown()

    // Use this for variables on the left side of an implication.
    // Returns null only if the variable is an unstable alias, and so cannot be used at all.
    fun getOrCreate(fir: FirElement, unwrapAlias: (RealVariable, FirElement) -> RealVariable?): DataFlowVariable? =
        getImpl(fir, createReal = true, unwrapAlias)?.rememberSynthetic()

    // Use this for calling `getTypeStatement` or accessing reassignment information.
    // Returns null if it's already known that no statements about the variable were made ever.
    fun getRealVariableWithoutUnwrappingAlias(fir: FirElement, unwrapAlias: (RealVariable, FirElement) -> RealVariable?): RealVariable? =
        getImpl(fir, createReal = false, unwrapAlias = { variable, _ -> variable }, unwrapAlias) as? RealVariable

    // Use this when adding statements to a variable initialization.
    // Returns null only if `fir` is not a variable declaration or assignment. Ideally, that shouldn't happen.
    fun getOrCreateRealVariableWithoutUnwrappingAlias(fir: FirElement, unwrapAlias: (RealVariable, FirElement) -> RealVariable?): RealVariable? =
        getImpl(fir, createReal = true, unwrapAlias = { variable, _ -> variable }, unwrapAlias) as? RealVariable

    // Use this for variables on the left side of an implication if `fir` is known to not be a variable access.
    // Equivalent to `getOrCreate` in those cases, but doesn't spend time validating the precondition.
    fun createSynthetic(fir: FirElement): SyntheticVariable =
        fir.unwrapElement().let { syntheticVariables.getOrPut(it) { SyntheticVariable(it, nextVariableIndex) } }


    // Looking up real variables has two "failure modes": when the FIR statement cannot have a real variable in the first place,
    // and when it could if not for `createReal = false`. Having one `null` value does not help us here, so to tell those two
    // situations apart this function has somewhat inconsistent return values:
    //   1. if `fir` maps to a real variable, and `createReal` is true, it returns `RealVariable`
    //      that IS in the `realVariables` map (possibly just added there);
    //   2. if `fir` maps to a real variable, but it's not in the map and `createReal` is false,
    //      OR if that variable is an unstable alias, it returns `null`;
    //   3. if `fir` does not map to a real variable, it returns a `SyntheticVariable`
    //      that IS NOT in the `syntheticVariables` map, so either `takeIfKnown` or `remember` should be called.
    // This way synthetic variables can always be recognized 100% precisely, but using this function requires a bit of care.
    private fun getImpl(
        fir: FirElement,
        createReal: Boolean,
        unwrapAlias: (RealVariable, FirElement) -> RealVariable?,
        unwrapAliasInReceivers: (RealVariable, FirElement) -> RealVariable? = unwrapAlias,
    ): DataFlowVariable? {
        val unwrapped = fir.unwrapElement()
        val synthetic = SyntheticVariable(unwrapped, nextVariableIndex)
        val symbol = unwrapped.extractSymbol() ?: return synthetic
        val qualifiedAccess = unwrapped as? FirQualifiedAccessExpression
        val dispatchReceiverVar = qualifiedAccess?.dispatchReceiver?.let {
            (getImpl(it, createReal, unwrapAliasInReceivers) ?: return null) as? RealVariable ?: return synthetic
        }
        val extensionReceiverVar = qualifiedAccess?.extensionReceiver?.let {
            (getImpl(it, createReal, unwrapAliasInReceivers) ?: return null) as? RealVariable ?: return synthetic
        }
        val isReceiver = unwrapped is FirThisReceiverExpression
        val identifier = Identifier(symbol, isReceiver, dispatchReceiverVar, extensionReceiverVar)
        val originalType = when (unwrapped) {
            is FirExpression -> unwrapped.resolvedType
            is FirVariableAssignment -> unwrapped.unwrapLValue()?.resolvedType
            is FirProperty -> unwrapped.returnTypeRef.coneType
            else -> null
        }
        val real = if (createReal) RealVariable(identifier, originalType, nextVariableIndex).remember() else realVariables[identifier]
            ?: return null
        return unwrapAlias(real, unwrapped)
    }

    private fun DataFlowVariable.takeSyntheticIfKnown(): DataFlowVariable? =
        if (this is SyntheticVariable) syntheticVariables[fir] else this

    private fun DataFlowVariable.rememberSynthetic(): DataFlowVariable =
        if (this is SyntheticVariable) syntheticVariables.getOrPut(fir) { this } else this

    private fun RealVariable.remember(): RealVariable =
        realVariables.getOrPut(identifier) {
            identifier.dispatchReceiver?.dependentVariables?.add(this)
            identifier.extensionReceiver?.dependentVariables?.add(this)
            this
        }

    fun copyRealVariableWithRemapping(variable: RealVariable, from: RealVariable, to: RealVariable): RealVariable {
        // Precondition: `variable in from.dependentVariables`, so at least one of the receivers is `from`.
        val newIdentifier = with(variable.identifier) {
            copy(
                dispatchReceiver = if (dispatchReceiver == from) to else dispatchReceiver,
                extensionReceiver = if (extensionReceiver == from) to else extensionReceiver,
            )
        }
        return RealVariable(newIdentifier, variable.originalType, nextVariableIndex).remember()
    }

    fun getOrPut(variable: RealVariable): RealVariable {
        val newIdentifier = with(variable.identifier) {
            copy(dispatchReceiver = dispatchReceiver?.let(::getOrPut), extensionReceiver = extensionReceiver?.let(::getOrPut))
        }
        return RealVariable(newIdentifier, variable.originalType, nextVariableIndex).remember()
    }

    private fun FirElement.extractSymbol(): FirBasedSymbol<*>? = when (this) {
        is FirResolvable -> calleeReference.symbol
        is FirDeclaration -> symbol
        is FirResolvedQualifier -> symbol?.fullyExpandedClass(session)
        else -> null
    }?.takeIf {
        this is FirThisReceiverExpression || it is FirClassSymbol || (it is FirVariableSymbol && it !is FirSyntheticPropertySymbol)
    }?.unwrapFakeOverridesIfNecessary()

    private fun FirBasedSymbol<*>?.unwrapFakeOverridesIfNecessary(): FirBasedSymbol<*>? {
        if (this !is FirCallableSymbol) return this
        // This is necessary only for sake of optimizations necessary because this is a really hot place.
        // Not having `dispatchReceiverType` means that this is a local/top-level property that can't be a fake override.
        // And at the same time, checking a field is much faster than a couple of attributes (0.3 secs at MT Full Kotlin)
        if (this.dispatchReceiverType == null) return this

        return this.unwrapFakeOverrides()
    }
}
