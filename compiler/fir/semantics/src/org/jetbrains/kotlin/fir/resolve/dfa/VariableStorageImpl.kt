/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.descriptors.ClassKind
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
        symbol: FirBasedSymbol<*>,
        fir: FirElement,
        unwrap: (RealVariable, FirElement) -> RealVariable?,
    ): RealVariable? {
        val realFir = fir.unwrapElement()
        val identifier = getIdentifierBySymbol(symbol, realFir, unwrap) ?: return null
        val stability = symbol.getStability(realFir)
        requireWithAttachment(stability != null, { "Stability for initialized variable always should be computable" }) {
            withFirSymbolEntry("symbol", symbol)
            withFirEntry("fir", fir)
            withEntry("identifier", identifier.toString())
        }

        return _realVariables[identifier] ?: createReal(identifier, realFir, stability, unwrap)
    }

    override fun getRealVariableWithoutUnwrappingAlias(
        fir: FirElement,
        unwrapAlias: (RealVariable, FirElement) -> RealVariable?,
    ): RealVariable? {
        val realFir = fir.unwrapElement()
        val symbol = realFir.extractSymbol() ?: return null
        if (symbol.getStability(realFir) == null) return null
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

        val stability = symbol?.getStability(realFir)
        if (stability == null) {
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
            createReal -> createReal(identifier, realFir, stability, unwrapAlias)
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
        stability: PropertyStability,
        unwrapAlias: (RealVariable, FirElement) -> RealVariable?,
    ): RealVariable? {
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

        val receiverVariable = receiver?.let { getOrCreate(it, unwrapAlias) ?: return null }
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
        (this as? FirExpression)?.unwrapSmartcastExpression() is FirThisReceiverExpression ||
                (it !is FirFunctionSymbol<*> && it !is FirSyntheticPropertySymbol)
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
