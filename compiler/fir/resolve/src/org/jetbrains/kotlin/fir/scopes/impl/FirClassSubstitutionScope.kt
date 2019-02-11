/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirMemberFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeAbbreviatedTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeImpl
import org.jetbrains.kotlin.name.Name

class FirClassSubstitutionScope(
    session: FirSession,
    private val unsubstituted: FirScope,
    private val substitution: Map<ConeTypeParameterSymbol, ConeKotlinType>,
    lookupInFir: Boolean
) : FirAbstractProviderBasedScope(session, lookupInFir) {

    val fakeOverrides = mutableMapOf<ConeCallableSymbol, ConeCallableSymbol>()

    private fun wrapProjection(old: ConeKotlinTypeProjection, newType: ConeKotlinType): ConeKotlinTypeProjection {
        return when (old) {
            is StarProjection -> old
            is ConeKotlinTypeProjectionIn -> ConeKotlinTypeProjectionIn(newType)
            is ConeKotlinTypeProjectionOut -> ConeKotlinTypeProjectionOut(newType)
            is ConeKotlinType -> newType
            else -> old
        }
    }

    private fun ConeKotlinType.substitute(): ConeKotlinType? {
        if (this is ConeTypeParameterType) return substitution[this]

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
                is ConeClassTypeImpl -> ConeClassTypeImpl(symbol, newArguments as Array<ConeKotlinTypeProjection>)
                is ConeAbbreviatedTypeImpl -> ConeAbbreviatedTypeImpl(
                    abbreviationSymbol,
                    newArguments as Array<ConeKotlinTypeProjection>,
                    directExpansion.substitute() as? ConeClassLikeType ?: directExpansion
                )
                is ConeFunctionType -> TODO("Substitute function type properly")
                is ConeClassLikeType -> error("Unknown class-like type to substitute: $this, ${this::class}")
            }
        }
        return null
    }


    override fun processFunctionsByName(name: Name, processor: (ConeFunctionSymbol) -> ProcessorAction): ProcessorAction {
        unsubstituted.processFunctionsByName(name) process@{ original ->

            val function = fakeOverrides.getOrPut(original) { createFakeOverride(original, name) }
            processor(function as ConeFunctionSymbol)
        }


        return super.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (ConePropertySymbol) -> ProcessorAction): ProcessorAction {
        return unsubstituted.processPropertiesByName(name, processor)
    }

    private fun createFakeOverride(
        original: ConeFunctionSymbol,
        name: Name
    ): FirFunctionSymbol {
        val member = (original as FirBasedSymbol<*>).fir as? FirNamedFunction ?: error("Can't fake override for $original")
        val receiverType = member.receiverType?.coneTypeUnsafe()
        val newReceiverType = receiverType?.substitute()

        val returnType = member.returnType.coneTypeUnsafe()
        val newReturnType = returnType.substitute()

        val newParameterTypes = member.valueParameters.map {
            it.returnType.coneTypeUnsafe().substitute()
        }

        val symbol = FirFunctionSymbol(original.callableId, true)
        with(member) {
            // TODO: consider using here some light-weight functions instead of pseudo-real FirMemberFunctionImpl
            // As second alternative, we can invent some light-weight kind of FirRegularClass
            FirMemberFunctionImpl(
                session,
                psi,
                symbol,
                name,
                member.receiverType?.withReplacedConeType(newReceiverType),
                member.returnType.withReplacedConeType(newReturnType)
            ).apply {
                status = member.status as FirDeclarationStatusImpl
                valueParameters += member.valueParameters.zip(newParameterTypes) { valueParameter, newType ->
                    with(valueParameter) {
                        FirValueParameterImpl(
                            session, psi,
                            name, this.returnType.withReplacedConeType(newType),
                            defaultValue, isCrossinline, isNoinline, isVararg
                        )
                    }
                }
            }
        }
        return symbol
    }
}


fun FirType.withReplacedConeType(newType: ConeKotlinType?): FirResolvedType {
    require(this is FirResolvedType)
    if (newType == null) return this

    return FirResolvedTypeImpl(
        session, psi, newType,
        isMarkedNullable,
        annotations
    )

}