/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

@OptIn(DfaInternals::class)
class VariableStorageImpl(private val session: FirSession) : VariableStorage() {
    private val realVariables: MutableMap<Identifier, RealVariable> = HashMap()
    private val syntheticVariables: MutableMap<FirElement, SyntheticVariable> = HashMap()

    private val nextVariableIndex: Int
        get() = realVariables.size + syntheticVariables.size + 1

    fun clear(): VariableStorageImpl = VariableStorageImpl(session)

    override fun getLocalVariable(symbol: FirBasedSymbol<*>, isReceiver: Boolean): RealVariable? =
        realVariables[Identifier(symbol, isReceiver, null, null)]

    fun getAllLocalVariables(): Iterable<RealVariable> =
        realVariables.values.filter { it.identifier.dispatchReceiver == null && it.identifier.extensionReceiver == null }

    // General pattern when using these functions:
    //
    //   val argumentVariable = variableStorage.{get,getOrCreateIfReal}(flow, fir.argument) ?: return
    //   val expressionVariable = variableStorage.createSynthetic(fir)
    //   flow.addImplication(somethingAbout(expressionVariable) implies somethingElseAbout(argumentVariable))
    //
    // If "something else" is a type/nullability statement, use `getOrCreateIfReal`; if it's `... == true/false`, use `get`.
    // The point is to only create variables and statements if they lead to useful conclusions; if a variable
    // does not exist, then no statements about it have been made, and if it's synthetic, none will be created later.
    override fun get(flow: Flow, fir: FirElement): DataFlowVariable? =
        get(flow, fir, createReal = false, whenReal = flow::unwrapVariable, whenSynthetic = { it.takeIfKnown() })

    fun getOrCreateIfReal(flow: Flow, fir: FirElement): DataFlowVariable? =
        get(flow, fir, createReal = true, whenReal = flow::unwrapVariable, whenSynthetic = { it.takeIfKnown() })

    fun getOrCreate(flow: Flow, fir: FirElement): DataFlowVariable =
        get(flow, fir, createReal = true, whenReal = flow::unwrapVariable, whenSynthetic = { it.remember() })!!

    fun getRealVariableWithoutUnwrappingAlias(flow: Flow, fir: FirElement): RealVariable? =
        get(flow, fir, createReal = false, whenReal = { it }, whenSynthetic = { null })

    fun getOrCreateRealVariableWithoutUnwrappingAlias(flow: Flow, fir: FirElement): RealVariable? =
        get(flow, fir, createReal = true, whenReal = { it }, whenSynthetic = { null })

    // NOTE: this does not do any checks; don't pass any elements that could map to real variables.
    fun createSynthetic(fir: FirElement): SyntheticVariable =
        SyntheticVariable(fir.unwrapElement(), nextVariableIndex).remember()

    private inline fun <R> get(
        flow: Flow, fir: FirElement, createReal: Boolean,
        whenReal: (RealVariable) -> R,
        whenSynthetic: (SyntheticVariable) -> R
    ): R? = when (val result = get(flow, fir, createReal)) {
        is RealVariable -> whenReal(result)
        is SyntheticVariable -> whenSynthetic(result)
        else -> null // only reachable if createReal is false and the variable would otherwise be real
    }

    // Looking up real variables has two "failure modes": when the FIR statement cannot have a real variable in the first place,
    // and when it could if not for `createReal = false`. Having one `null` value does not help us here, so to tell those two
    // situations apart this function has somewhat inconsistent return values:
    //   1. if `fir` maps to a real variable, and `createReal` is true, it returns `RealVariable`
    //      that IS in the `realVariables` map (possibly just added there);
    //   2. if `fir` maps to a real variable, but it's not in the map and `createReal` is false, it returns `null`;
    //   3. if `fir` does not map to a real variable, it returns a `SyntheticVariable`
    //      that IS NOT in the `syntheticVariables` map, so either `takeIfKnown` or `remember` should be called.
    // This way synthetic variables can always be recognized 100% precisely, but using this function requires a bit of care.
    private fun get(flow: Flow, fir: FirElement, createReal: Boolean): DataFlowVariable? {
        val unwrapped = fir.unwrapElement()
        val synthetic = SyntheticVariable(unwrapped, nextVariableIndex)
        val symbol = unwrapped.symbol ?: return synthetic
        val qualifiedAccess = unwrapped as? FirQualifiedAccessExpression
        val stability = symbol.getStability(qualifiedAccess) ?: return synthetic
        val dispatchReceiverVar = qualifiedAccess?.dispatchReceiver?.let {
            get(flow, it, createReal, whenReal = flow::unwrapVariable, whenSynthetic = { return synthetic }) ?: return null
        }
        val extensionReceiverVar = qualifiedAccess?.extensionReceiver?.let {
            get(flow, it, createReal, whenReal = flow::unwrapVariable, whenSynthetic = { return synthetic }) ?: return null
        }
        val isReceiver = qualifiedAccess?.calleeReference is FirThisReference
        val identifier = Identifier(symbol, isReceiver, dispatchReceiverVar, extensionReceiverVar)
        val combinedStability = stability
            .combineWithReceiverStability(dispatchReceiverVar?.stability)
            .combineWithReceiverStability(extensionReceiverVar?.stability)
        return if (createReal) RealVariable(identifier, combinedStability, nextVariableIndex).remember() else realVariables[identifier]
    }

    private fun SyntheticVariable.takeIfKnown(): SyntheticVariable? =
        syntheticVariables[fir]

    private fun SyntheticVariable.remember(): SyntheticVariable =
        syntheticVariables.getOrPut(fir) { this }

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
        return RealVariable(newIdentifier, variable.stability, nextVariableIndex).remember()
    }

    fun getOrPut(variable: RealVariable): RealVariable {
        val newIdentifier = with(variable.identifier) {
            copy(dispatchReceiver = dispatchReceiver?.let(::getOrPut), extensionReceiver = extensionReceiver?.let(::getOrPut))
        }
        return RealVariable(newIdentifier, variable.stability, nextVariableIndex).remember()
    }

    private fun FirBasedSymbol<*>.getStability(qualifiedAccess: FirQualifiedAccessExpression?): PropertyStability? {
        if (qualifiedAccess is FirThisReceiverExpression) return PropertyStability.STABLE_VALUE
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
            property.modality != Modality.FINAL -> {
                val dispatchReceiver = qualifiedAccess?.dispatchReceiver ?: return null
                val receiverType = dispatchReceiver.resolvedType.lowerBoundIfFlexible().fullyExpandedType(session)
                val receiverSymbol = (receiverType as? ConeClassLikeType)?.lookupTag?.toSymbol(session) ?: return null
                when (val receiverFir = receiverSymbol.fir) {
                    is FirAnonymousObject -> PropertyStability.STABLE_VALUE
                    is FirRegularClass -> if (receiverFir.modality == Modality.FINAL) PropertyStability.STABLE_VALUE else PropertyStability.PROPERTY_WITH_GETTER
                    else -> errorWithAttachment("Should not be here: $${receiverFir::class.simpleName}") {
                        withFirEntry("fir", receiverFir)
                    }
                }
            }
            else -> property.determineStabilityByModule()
        }
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

private val FirElement.symbol: FirBasedSymbol<*>?
    get() = when (this) {
        is FirDeclaration -> symbol.unwrapFakeOverridesIfNecessary()
        is FirResolvable -> calleeReference.symbol.unwrapFakeOverridesIfNecessary()
        is FirResolvedQualifier -> symbol
        else -> null
    }?.takeIf {
        this is FirThisReceiverExpression || (it !is FirFunctionSymbol<*> && it !is FirSyntheticPropertySymbol)
    }

private fun FirBasedSymbol<*>?.unwrapFakeOverridesIfNecessary(): FirBasedSymbol<*>? {
    if (this !is FirCallableSymbol) return this
    // This is necessary only for sake of optimizations because this is a really hot place.
    // Not having `dispatchReceiverType` means that this is a local/top-level property that can't be a fake override.
    // And at the same time, checking a field is much faster than a couple of attributes (0.3 secs at MT Full Kotlin)
    if (this.dispatchReceiverType == null) return this
    return this.unwrapFakeOverrides()
}
