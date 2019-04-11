/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirMemberFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorWithJump
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeAbbreviatedTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.name.Name

class FirClassSubstitutionScope(
    private val session: FirSession,
    private val useSiteScope: FirScope,
    private val substitution: Map<ConeTypeParameterSymbol, ConeKotlinType>
) : FirScope {

    private val fakeOverrides = mutableMapOf<ConeCallableSymbol, ConeCallableSymbol>()

    private fun wrapProjection(old: ConeKotlinTypeProjection, newType: ConeKotlinType): ConeKotlinTypeProjection {
        return when (old) {
            is ConeStarProjection -> old
            is ConeKotlinTypeProjectionIn -> ConeKotlinTypeProjectionIn(newType)
            is ConeKotlinTypeProjectionOut -> ConeKotlinTypeProjectionOut(newType)
            is ConeKotlinType -> newType
            else -> old
        }
    }

    private fun ConeKotlinType.substitute(): ConeKotlinType? {
        if (this is ConeTypeParameterType) return substitution[lookupTag]

        val newArguments by lazy { arrayOfNulls<ConeKotlinTypeProjection>(typeArguments.size) }
        var initialized = false
        for ((index, typeArgument) in this.typeArguments.withIndex()) {
            val type = (typeArgument as? ConeTypedProjection)?.type ?: continue
            val newType = type.substitute()
            if (newType != null) {
                initialized = true
                newArguments[index] = wrapProjection(typeArgument, newType)
            }
        }

        if (initialized) {
            for ((index, typeArgument) in this.typeArguments.withIndex()) {
                if (newArguments[index] == null) {
                    newArguments[index] = typeArgument
                }
            }
            @Suppress("UNCHECKED_CAST")
            return when (this) {
                is ConeKotlinErrorType -> error("Trying to substitute arguments for error type")
                is ConeTypeParameterType -> error("Trying to substitute arguments for type parameter")
                is ConeClassTypeImpl -> ConeClassTypeImpl(
                    lookupTag,
                    newArguments as Array<ConeKotlinTypeProjection>,
                    nullability.isNullable
                )
                is ConeAbbreviatedTypeImpl -> ConeAbbreviatedTypeImpl(
                    abbreviationLookupTag,
                    newArguments as Array<ConeKotlinTypeProjection>,
                    nullability.isNullable
                )
                is ConeFunctionType -> TODO("Substitute function type properly")
                is ConeClassLikeType -> error("Unknown class-like type to substitute: $this, ${this::class}")
                is ConeFlexibleType -> error("Trying to substitute arguments for flexible type")
                is ConeCapturedType -> error("Not supported")
            }
        }
        return null
    }


    override fun processFunctionsByName(name: Name, processor: (ConeFunctionSymbol) -> ProcessorAction): ProcessorAction {
        useSiteScope.processFunctionsByName(name) process@{ original ->

            val function = fakeOverrides.getOrPut(original) { createFakeOverride(original) }
            processor(function as ConeFunctionSymbol)
        }


        return super.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (ConeVariableSymbol) -> ProcessorAction): ProcessorAction {
        return useSiteScope.processPropertiesByName(name, processor)
    }

    private val typeCalculator by lazy { ReturnTypeCalculatorWithJump(session) }

    private fun createFakeOverride(original: ConeFunctionSymbol): FirFunctionSymbol {

        val member = original.firUnsafe<FirFunction>()
        if (member is FirConstructor) return original as FirFunctionSymbol // TODO: substitution for constructors
        member as FirNamedFunction

        val receiverType = member.receiverTypeRef?.coneTypeUnsafe()
        val newReceiverType = receiverType?.substitute()

        val returnType = typeCalculator.tryCalculateReturnType(member).type
        val newReturnType = returnType.substitute()

        val newParameterTypes = member.valueParameters.map {
            it.returnTypeRef.coneTypeUnsafe().substitute()
        }

        return createFakeOverride(session, member, original as FirFunctionSymbol, newReceiverType, newReturnType, newParameterTypes)
    }

    companion object {
        fun createFakeOverride(
            session: FirSession,
            baseFunction: FirNamedFunction,
            baseSymbol: FirFunctionSymbol,
            newReceiverType: ConeKotlinType? = null,
            newReturnType: ConeKotlinType? = null,
            newParameterTypes: List<ConeKotlinType?>? = null
        ): FirFunctionSymbol {
            val symbol = FirFunctionSymbol(baseSymbol.callableId, true, baseSymbol)
            with(baseFunction) {
                // TODO: consider using here some light-weight functions instead of pseudo-real FirMemberFunctionImpl
                // As second alternative, we can invent some light-weight kind of FirRegularClass
                FirMemberFunctionImpl(
                    session,
                    psi, symbol, name,
                    baseFunction.receiverTypeRef?.withReplacedConeType(session, newReceiverType),
                    baseFunction.returnTypeRef.withReplacedConeType(session, newReturnType)
                ).apply {
                    status = baseFunction.status as FirDeclarationStatusImpl
                    valueParameters += baseFunction.valueParameters.zip(
                        newParameterTypes ?: List(baseFunction.valueParameters.size) { null }
                    ) { valueParameter, newType ->
                        with(valueParameter) {
                            FirValueParameterImpl(
                                session, psi,
                                name, this.returnTypeRef.withReplacedConeType(session, newType),
                                defaultValue, isCrossinline, isNoinline, isVararg,
                                FirVariableSymbol(valueParameter.symbol.callableId)
                            )
                        }
                    }
                }
            }
            return symbol
        }
    }
}


fun FirTypeRef.withReplacedConeType(session: FirSession, newType: ConeKotlinType?): FirResolvedTypeRef {
    require(this is FirResolvedTypeRef)
    if (newType == null) return this

    return FirResolvedTypeRefImpl(
        session, psi, newType,
        isMarkedNullable,
        annotations
    )

}
