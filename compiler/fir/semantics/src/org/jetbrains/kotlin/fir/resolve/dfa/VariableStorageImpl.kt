/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
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
import org.jetbrains.kotlin.types.SmartcastStability

class VariableStorageImpl(private val session: FirSession) : VariableStorage() {
    // These are basically hash sets, since they map each key to itself. The only point of having them as maps
    // is to deduplicate equal instances with lookups. The impact of that is questionable, but whatever.
    private val realVariables: MutableMap<RealVariable, RealVariable> = HashMap()
    private val syntheticVariables: MutableMap<FirElement, SyntheticVariable> = HashMap()

    private val nextVariableIndex: Int
        get() = realVariables.size + syntheticVariables.size + 1

    fun clear(): VariableStorageImpl = VariableStorageImpl(session)

    override fun getLocalVariable(symbol: FirBasedSymbol<*>, isReceiver: Boolean): RealVariable? =
        RealVariable(symbol, isReceiver, null, null, SmartcastStability.STABLE_VALUE, nextVariableIndex).takeIfKnown()

    fun getOrCreateLocalVariable(symbol: FirBasedSymbol<*>, isReceiver: Boolean): RealVariable =
        RealVariable(symbol, isReceiver, null, null, SmartcastStability.STABLE_VALUE, nextVariableIndex).remember()

    fun getAllLocalVariables(): Iterable<RealVariable> =
        realVariables.values.filter { it.dispatchReceiver == null && it.extensionReceiver == null }

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
        val isReceiver = qualifiedAccess?.calleeReference is FirThisReference
        val stability = if (isReceiver) SmartcastStability.STABLE_VALUE else symbol.getStability(qualifiedAccess) ?: return synthetic
        val dispatchReceiverVar = qualifiedAccess?.dispatchReceiver?.let {
            get(flow, it, createReal, whenReal = flow::unwrapVariable, whenSynthetic = { return synthetic }) ?: return null
        }
        val extensionReceiverVar = qualifiedAccess?.extensionReceiver?.let {
            get(flow, it, createReal, whenReal = flow::unwrapVariable, whenSynthetic = { return synthetic }) ?: return null
        }
        val combinedStability = stability
            .combineWithReceiverStability(dispatchReceiverVar?.stability)
            .combineWithReceiverStability(extensionReceiverVar?.stability)
        val real = RealVariable(symbol, isReceiver, dispatchReceiverVar, extensionReceiverVar, combinedStability, nextVariableIndex)
        return if (createReal) real.remember() else real.takeIfKnown()
    }

    private fun SmartcastStability.combineWithReceiverStability(receiverStability: SmartcastStability?): SmartcastStability {
        if (receiverStability == null) return this
        return maxOf(this, receiverStability)
    }

    private fun SyntheticVariable.takeIfKnown(): SyntheticVariable? =
        syntheticVariables[fir]

    private fun SyntheticVariable.remember(): SyntheticVariable =
        syntheticVariables.getOrPut(fir) { this }

    private fun RealVariable.takeIfKnown(): RealVariable? =
        realVariables[this]

    private fun RealVariable.remember(): RealVariable =
        realVariables.getOrPut(this) {
            dispatchReceiver?.dependentVariables?.add(this)
            extensionReceiver?.dependentVariables?.add(this)
            this
        }

    fun copyRealVariableWithRemapping(variable: RealVariable, from: RealVariable, to: RealVariable): RealVariable {
        // Precondition: `variable in from.dependentVariables`, so at least one of the receivers is `from`.
        return with(variable) {
            val newDispatchReceiver = if (dispatchReceiver == from) to else dispatchReceiver
            val newExtensionReceiver = if (extensionReceiver == from) to else extensionReceiver
            RealVariable(symbol, isReceiver, newDispatchReceiver, newExtensionReceiver, stability, nextVariableIndex).remember()
        }
    }

    fun getOrPut(variable: RealVariable): RealVariable {
        return with(variable) {
            val newDispatchReceiver = dispatchReceiver?.let(::getOrPut)
            val newExtensionReceiver = extensionReceiver?.let(::getOrPut)
            RealVariable(symbol, isReceiver, newDispatchReceiver, newExtensionReceiver, stability, nextVariableIndex).remember()
        }
    }

    private fun FirBasedSymbol<*>.getStability(qualifiedAccess: FirQualifiedAccessExpression?): SmartcastStability? {
        return when (val fir = fir) {
            is FirRegularClass -> SmartcastStability.STABLE_VALUE // named object or containing class for a static field reference
            !is FirVariable -> null
            is FirSyntheticProperty -> null
            is FirEnumEntry -> SmartcastStability.STABLE_VALUE
            is FirErrorProperty -> SmartcastStability.STABLE_VALUE
            is FirValueParameter -> SmartcastStability.STABLE_VALUE
            is FirBackingField -> if (fir.isVal) SmartcastStability.STABLE_VALUE else SmartcastStability.MUTABLE_PROPERTY
            is FirField -> when {
                !fir.isFinal -> SmartcastStability.MUTABLE_PROPERTY
                !fir.isInCurrentOrFriendModule() -> SmartcastStability.ALIEN_PUBLIC_PROPERTY
                else -> SmartcastStability.STABLE_VALUE
            }
            is FirProperty -> when {
                fir.isExpect -> SmartcastStability.EXPECT_PROPERTY
                fir.delegate != null -> SmartcastStability.DELEGATED_PROPERTY
                // Local vars are only *sometimes* unstable (when there are concurrent assignments). `FirDataFlowAnalyzer`
                // will check that at each use site individually and produce `CAPTURED_VARIABLE` instead when necessary.
                fir.isLocal -> SmartcastStability.STABLE_VALUE
                fir.isVar -> SmartcastStability.MUTABLE_PROPERTY
                fir.receiverParameter != null -> SmartcastStability.PROPERTY_WITH_GETTER
                fir.getter !is FirDefaultPropertyAccessor? -> SmartcastStability.PROPERTY_WITH_GETTER
                fir.visibility == Visibilities.Private -> SmartcastStability.STABLE_VALUE
                !fir.isFinal && qualifiedAccess?.hasFinalDispatchReceiver() != true -> SmartcastStability.PROPERTY_WITH_GETTER
                !fir.isInCurrentOrFriendModule() -> SmartcastStability.ALIEN_PUBLIC_PROPERTY
                else -> SmartcastStability.STABLE_VALUE
            }
        }
    }

    private fun FirQualifiedAccessExpression.hasFinalDispatchReceiver(): Boolean {
        val receiverType = dispatchReceiver?.resolvedType?.lowerBoundIfFlexible()?.fullyExpandedType(session) ?: return false
        val receiverFir = (receiverType as? ConeClassLikeType)?.lookupTag?.toSymbol(session)?.fir ?: return false
        return receiverFir is FirAnonymousObject || (receiverFir is FirRegularClass && receiverFir.isFinal)
    }

    private fun FirVariable.isInCurrentOrFriendModule(): Boolean {
        val propertyModuleData = originalOrSelf().moduleData
        val currentModuleData = session.moduleData
        return propertyModuleData == currentModuleData ||
                propertyModuleData in currentModuleData.friendDependencies ||
                propertyModuleData in currentModuleData.allDependsOnDependencies
    }
}

// The meaning of this function is this: if you have two expressions and in all possible
// executions either the two produce equal values or one of them throws, then these two
// expressions are interchangeable in type statements. For example, saying that `x` is
// a String is equivalent to saying that `x as T` is a String, as the two have identical
// runtime values (unless `x as T` throws, in which case everything is true anyway).
//
// This equivalence relation produces equivalence classes on FIR expressions, and
// `unwrapElement` does a "best attempt" at providing a consistent representative for an
// expression's equivalence class. It can fail to do so and return different representatives
// for different expressions in the same class, which will make DFA miss some smart casts.
// That's not great, but acceptable. What's not acceptable is returning expressions from
// other equivalence classes: that would create incorrect smart casts, and that's bad.
private tailrec fun FirElement.unwrapElement(): FirElement {
    return when (this) {
        is FirWhenSubjectExpression -> whenRef.value.let { it.subjectVariable ?: it.subject ?: return this }.unwrapElement()
        is FirSmartCastExpression -> originalExpression.unwrapElement()
        is FirSafeCallExpression -> selector.unwrapElement()
        is FirCheckedSafeCallSubject -> originalReceiverRef.value.unwrapElement()
        is FirCheckNotNullCall -> argument.unwrapElement()
        is FirDesugaredAssignmentValueReferenceExpression -> expressionRef.value.unwrapElement()
        is FirVariableAssignment -> lValue.unwrapElement()
        is FirTypeOperatorCall -> if (operation == FirOperation.AS) argument.unwrapElement() else this
        else -> this
    }
}

private val FirElement.symbol: FirBasedSymbol<*>?
    get() = when (this) {
        is FirDeclaration -> symbol.unwrapFakeOverridesIfNecessary()
        is FirResolvable -> calleeReference.symbol.unwrapFakeOverridesIfNecessary()
        is FirResolvedQualifier -> symbol
        // Only called on the result of `unwrapElement, so handling all those cases here is redundant.
        else -> null
    }

private fun FirBasedSymbol<*>?.unwrapFakeOverridesIfNecessary(): FirBasedSymbol<*>? {
    if (this !is FirCallableSymbol) return this
    // This is necessary only for sake of optimizations because this is a really hot place.
    // Not having `dispatchReceiverType` means that this is a local/top-level property that can't be a fake override.
    // And at the same time, checking a field is much faster than a couple of attributes (0.3 secs at MT Full Kotlin)
    if (this.dispatchReceiverType == null) return this
    return this.unwrapFakeOverrides()
}
