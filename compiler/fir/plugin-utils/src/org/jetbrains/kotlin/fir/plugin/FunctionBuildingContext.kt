/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.toFirResolvedTypeRef
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

public sealed class FunctionBuildingContext<T : FirFunction>(
    protected val callableId: CallableId,
    session: FirSession,
    key: GeneratedDeclarationKey,
    owner: FirClassSymbol<*>?
) : DeclarationBuildingContext<T>(session, key, owner) {
    protected data class ValueParameterData(
        val name: Name,
        val typeProvider: (List<FirTypeParameterRef>) -> ConeKotlinType,
        val isCrossinline: Boolean,
        val isNoinline: Boolean,
        val isVararg: Boolean,
        val hasDefaultValue: Boolean,
        val key: GeneratedDeclarationKey
    )

    protected val valueParameters: MutableList<ValueParameterData> = mutableListOf()

    /**
     * Adds value parameter with [type] type to constructed function
     *
     * If you set [hasDefaultValue] to true then you need to generate actual default value
     *   in [IrGenerationExtension]
     */
    public fun valueParameter(
        name: Name,
        type: ConeKotlinType,
        isCrossinline: Boolean = false,
        isNoinline: Boolean = false,
        isVararg: Boolean = false,
        hasDefaultValue: Boolean = false,
        key: GeneratedDeclarationKey = this@FunctionBuildingContext.key
    ) {
        valueParameter(name, { type }, isCrossinline, isNoinline, isVararg, hasDefaultValue, key)
    }

    /**
     * Adds value parameter with type provided by [typeProvider] to constructed function
     * Use this overload when parameter type uses type parameters of constructed declaration
     *
     * If you set [hasDefaultValue] to true then you need to generate actual default value
     *   in [IrGenerationExtension]
     */
    public fun valueParameter(
        name: Name,
        typeProvider: (List<FirTypeParameterRef>) -> ConeKotlinType,
        isCrossinline: Boolean = false,
        isNoinline: Boolean = false,
        isVararg: Boolean = false,
        hasDefaultValue: Boolean = false,
        key: GeneratedDeclarationKey = this@FunctionBuildingContext.key
    ) {
        valueParameters += ValueParameterData(name, typeProvider, isCrossinline, isNoinline, isVararg, hasDefaultValue, key)
    }

    protected fun generateValueParameter(
        valueParameter: ValueParameterData,
        containingFunctionSymbol: FirFunctionSymbol<*>,
        functionTypeParameters: List<FirTypeParameterRef>
    ): FirValueParameter {
        return buildValueParameter {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = valueParameter.key.origin
            returnTypeRef = valueParameter.typeProvider.invoke(functionTypeParameters).toFirResolvedTypeRef()
            name = valueParameter.name
            symbol = FirValueParameterSymbol(name)
            if (valueParameter.hasDefaultValue) {
                // TODO: check how it will actually work in fir2ir
                defaultValue = buildExpressionStub { coneTypeOrNull = session.builtinTypes.nothingType.type }
            }
            this.containingFunctionSymbol = containingFunctionSymbol
            isCrossinline = valueParameter.isCrossinline
            isNoinline = valueParameter.isNoinline
            isVararg = valueParameter.isVararg
        }
    }
}
