/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.impl.*
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
    private val useSiteScope: FirScope,
    scopeSession: ScopeSession,
    substitution: Map<FirTypeParameterSymbol, ConeKotlinType>
) : FirScope() {

    private val fakeOverrideFunctions = mutableMapOf<FirFunctionSymbol<*>, FirFunctionSymbol<*>>()
    private val fakeOverrideProperties = mutableMapOf<FirPropertySymbol, FirPropertySymbol>()

    private val substitutor = substitutorByMap(substitution)

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> ProcessorAction): ProcessorAction {
        useSiteScope.processFunctionsByName(name) process@{ original ->

            val function = fakeOverrideFunctions.getOrPut(original) { createFakeOverrideFunction(original) }
            processor(function)
        }


        return super.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> ProcessorAction): ProcessorAction {
        return useSiteScope.processPropertiesByName(name) process@{ original ->
            if (original is FirPropertySymbol) {
                val property = fakeOverrideProperties.getOrPut(original) { createFakeOverrideProperty(original) }
                processor(property)
            } else {
                processor(original)
            }
        }
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

    companion object {
        fun createFakeOverrideFunction(
            session: FirSession,
            baseFunction: FirNamedFunction,
            baseSymbol: FirNamedFunctionSymbol,
            newReceiverType: ConeKotlinType? = null,
            newReturnType: ConeKotlinType? = null,
            newParameterTypes: List<ConeKotlinType?>? = null
        ): FirNamedFunctionSymbol {
            val symbol = FirNamedFunctionSymbol(baseSymbol.callableId, true, baseSymbol)
            with(baseFunction) {
                // TODO: consider using here some light-weight functions instead of pseudo-real FirMemberFunctionImpl
                // As second alternative, we can invent some light-weight kind of FirRegularClass
                FirMemberFunctionImpl(
                    session,
                    psi, symbol, name,
                    baseFunction.receiverTypeRef?.withReplacedConeType(newReceiverType),
                    baseFunction.returnTypeRef.withReplacedConeType(newReturnType)
                ).apply {
                    resolvePhase = baseFunction.resolvePhase
                    status = baseFunction.status as FirDeclarationStatusImpl
                    valueParameters += baseFunction.valueParameters.zip(
                        newParameterTypes ?: List(baseFunction.valueParameters.size) { null }
                    ) { valueParameter, newType ->
                        with(valueParameter) {
                            FirValueParameterImpl(
                                session, psi,
                                name, this.returnTypeRef.withReplacedConeType(newType),
                                defaultValue, isCrossinline, isNoinline, isVararg,
                                FirVariableSymbol(valueParameter.symbol.callableId)
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
                FirMemberPropertyImpl(
                    session,
                    psi, symbol, name,
                    baseProperty.receiverTypeRef?.withReplacedConeType(newReceiverType),
                    baseProperty.returnTypeRef.withReplacedConeType(newReturnType),
                    isVar, initializer = null, delegate = null
                ).apply {
                    resolvePhase = baseProperty.resolvePhase
                    status = baseProperty.status as FirDeclarationStatusImpl
                }
            }
            return symbol
        }
    }
}


fun FirTypeRef.withReplacedConeType(newType: ConeKotlinType?): FirResolvedTypeRef {
    require(this is FirResolvedTypeRef)
    if (newType == null) return this

    return FirResolvedTypeRefImpl(psi, newType, annotations = annotations)

}
