/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirFieldImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirPropertyImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirSimpleFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorWithJump
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.name.Name

class FirClassSubstitutionScope(
    private val session: FirSession,
    private val useSiteMemberScope: FirScope,
    scopeSession: ScopeSession,
    substitution: Map<FirTypeParameterSymbol, ConeKotlinType>
) : FirScope() {

    private val fakeOverrideFunctions = mutableMapOf<FirFunctionSymbol<*>, FirFunctionSymbol<*>>()
    private val fakeOverrideProperties = mutableMapOf<FirPropertySymbol, FirPropertySymbol>()
    private val fakeOverrideFields = mutableMapOf<FirFieldSymbol, FirFieldSymbol>()

    private val substitutor = substitutorByMap(substitution)

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> ProcessorAction): ProcessorAction {
        useSiteMemberScope.processFunctionsByName(name) process@{ original ->

            val function = fakeOverrideFunctions.getOrPut(original) { createFakeOverrideFunction(original) }
            processor(function)
        }


        return super.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> ProcessorAction): ProcessorAction {
        return useSiteMemberScope.processPropertiesByName(name) process@{ original ->
            when (original) {
                is FirPropertySymbol -> {
                    val property = fakeOverrideProperties.getOrPut(original) { createFakeOverrideProperty(original) }
                    processor(property)
                }
                is FirFieldSymbol -> {
                    val field = fakeOverrideFields.getOrPut(original) { createFakeOverrideField(original) }
                    processor(field)
                }
                else -> {
                    processor(original)
                }
            }
        }
    }

    override fun processClassifiersByName(name: Name, processor: (FirClassifierSymbol<*>) -> ProcessorAction): ProcessorAction {
        return useSiteMemberScope.processClassifiersByName(name, processor)
    }

    private val typeCalculator by lazy { ReturnTypeCalculatorWithJump(session, scopeSession) }

    private fun ConeKotlinType.substitute(): ConeKotlinType? {
        return substitutor.substituteOrNull(this)
    }

    private fun createFakeOverrideFunction(original: FirFunctionSymbol<*>): FirFunctionSymbol<*> {
        val member = when (original) {
            is FirNamedFunctionSymbol -> original.fir
            is FirConstructorSymbol -> return original
            else -> throw AssertionError("Should not be here")
        }

        val receiverType = member.receiverTypeRef?.coneTypeUnsafe<ConeKotlinType>()
        val newReceiverType = receiverType?.substitute()

        val returnType = typeCalculator.tryCalculateReturnType(member).type
        val newReturnType = returnType.substitute()

        val newParameterTypes = member.valueParameters.map {
            it.returnTypeRef.coneTypeUnsafe<ConeKotlinType>().substitute()
        }

        if (newReceiverType == null && newReturnType == null && newParameterTypes.all { it == null }) {
            return original
        }

        return createFakeOverrideFunction(session, member, original, newReceiverType, newReturnType, newParameterTypes)
    }

    private fun createFakeOverrideProperty(original: FirPropertySymbol): FirPropertySymbol {
        val member = original.fir

        val receiverType = member.receiverTypeRef?.coneTypeUnsafe<ConeKotlinType>()
        val newReceiverType = receiverType?.substitute()

        val returnType = typeCalculator.tryCalculateReturnType(member).type
        val newReturnType = returnType.substitute()

        if (newReceiverType == null && newReturnType == null) {
            return original
        }

        return createFakeOverrideProperty(session, member, original, newReceiverType, newReturnType)
    }

    private fun createFakeOverrideField(original: FirFieldSymbol): FirFieldSymbol {
        val member = original.fir

        val returnType = typeCalculator.tryCalculateReturnType(member).type
        val newReturnType = returnType.substitute() ?: return original

        return createFakeOverrideField(session, member, original, newReturnType)
    }

    companion object {
        fun createFakeOverrideFunction(
            session: FirSession,
            baseFunction: FirSimpleFunction,
            baseSymbol: FirNamedFunctionSymbol,
            newReceiverType: ConeKotlinType? = null,
            newReturnType: ConeKotlinType? = null,
            newParameterTypes: List<ConeKotlinType?>? = null
        ): FirNamedFunctionSymbol {
            val symbol = FirNamedFunctionSymbol(baseSymbol.callableId, true, baseSymbol)
            with(baseFunction) {
                // TODO: consider using here some light-weight functions instead of pseudo-real FirMemberFunctionImpl
                // As second alternative, we can invent some light-weight kind of FirRegularClass
                FirSimpleFunctionImpl(
                    source,
                    session,
                    baseFunction.returnTypeRef.withReplacedConeType(newReturnType),
                    baseFunction.receiverTypeRef?.withReplacedConeType(newReceiverType),
                    name,
                    baseFunction.status,
                    symbol
                ).apply {
                    resolvePhase = baseFunction.resolvePhase
                    valueParameters += baseFunction.valueParameters.zip(
                        newParameterTypes ?: List(baseFunction.valueParameters.size) { null }
                    ) { valueParameter, newType ->
                        with(valueParameter) {
                            FirValueParameterImpl(
                                source,
                                session,
                                this.returnTypeRef.withReplacedConeType(newType),
                                name,
                                FirVariableSymbol(valueParameter.symbol.callableId),
                                defaultValue,
                                isCrossinline,
                                isNoinline,
                                isVararg
                            )
                        }
                    }
                }
            }
            return symbol
        }

        fun createFakeOverrideProperty(
            session: FirSession,
            baseProperty: FirProperty,
            baseSymbol: FirPropertySymbol,
            newReceiverType: ConeKotlinType? = null,
            newReturnType: ConeKotlinType? = null
        ): FirPropertySymbol {
            val symbol = FirPropertySymbol(baseSymbol.callableId, true, baseSymbol)
            with(baseProperty) {
                FirPropertyImpl(
                    source,
                    session,
                    baseProperty.returnTypeRef.withReplacedConeType(newReturnType),
                    baseProperty.receiverTypeRef?.withReplacedConeType(newReceiverType),
                    name,
                    null,
                    null,
                    isVar,
                    symbol,
                    false,
                    baseProperty.status
                ).apply {
                    resolvePhase = baseProperty.resolvePhase
                }
            }
            return symbol
        }

        fun createFakeOverrideField(
            session: FirSession,
            baseField: FirField,
            baseSymbol: FirFieldSymbol,
            newReturnType: ConeKotlinType? = null
        ): FirFieldSymbol {
            val symbol = FirFieldSymbol(baseSymbol.callableId)
            with(baseField) {
                FirFieldImpl(
                    source, session,
                    baseField.returnTypeRef.withReplacedConeType(newReturnType),
                    name, symbol, isVar, baseField.status
                ).apply {
                    resolvePhase = baseField.resolvePhase
                }
            }
            return symbol
        }
    }
}


fun FirTypeRef.withReplacedConeType(newType: ConeKotlinType?): FirResolvedTypeRef {
    require(this is FirResolvedTypeRef)
    if (newType == null) return this

    return FirResolvedTypeRefImpl(source, newType).apply {
        annotations += this@withReplacedConeType.annotations
    }

}
