/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildReceiverParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.toFirResolvedTypeRef
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

public class SimpleFunctionBuildingContext(
    session: FirSession,
    key: GeneratedDeclarationKey,
    owner: FirClassSymbol<*>?,
    callableId: CallableId,
    private val returnTypeProvider: (List<FirTypeParameter>) -> ConeKotlinType,
) : FunctionBuildingContext<FirSimpleFunction>(callableId, session, key, owner) {
    private var extensionReceiverTypeProvider: ((List<FirTypeParameter>) -> ConeKotlinType)? = null

    /**
     * Sets [type] as extension receiver type of constructed function
     */
    public fun extensionReceiverType(type: ConeKotlinType) {
        extensionReceiverType { type }
    }

    /**
     * Sets type, provided by [typeProvider], as extension receiver type of constructed function
     *
     * Use this overload when extension receiver type uses type parameters of constructed function
     */
    public fun extensionReceiverType(typeProvider: (List<FirTypeParameter>) -> ConeKotlinType) {
        require(extensionReceiverTypeProvider == null) { "Extension receiver type is already initialized" }
        extensionReceiverTypeProvider = typeProvider
    }

    override fun build(): FirSimpleFunction {
        return buildSimpleFunction {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = key.origin

            symbol = FirNamedFunctionSymbol(callableId)
            name = callableId.callableName

            status = generateStatus()

            dispatchReceiverType = owner?.defaultType()

            this@SimpleFunctionBuildingContext.typeParameters.mapTo(typeParameters) {
                generateTypeParameter(it, symbol)
            }
            initTypeParameterBounds(typeParameters, typeParameters)
            produceContextReceiversTo(contextReceivers, typeParameters)

            this@SimpleFunctionBuildingContext.valueParameters.mapTo(valueParameters) {
                generateValueParameter(it, symbol, typeParameters)
            }
            returnTypeRef = returnTypeProvider(typeParameters).toFirResolvedTypeRef()
            extensionReceiverTypeProvider?.invoke(typeParameters)?.let {
                receiverParameter = buildReceiverParameter {
                    typeRef = it.toFirResolvedTypeRef()
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------

/**
 * Creates a member function for [owner] class with [returnType] return type
 *
 * Type and value parameters can be configured with [config] builder lambda
 */
public fun FirExtension.createMemberFunction(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    name: Name,
    returnType: ConeKotlinType,
    config: SimpleFunctionBuildingContext.() -> Unit = {}
): FirSimpleFunction {
    return createMemberFunction(owner, key, name, { returnType }, config)
}

/**
 * Creates a member function for [owner] class with return type provided by [returnTypeProvider]
 * Use this overload when return type uses type parameters of constructed property
 *
 * Type and value parameters can be configured with [config] builder lambda
 */
public fun FirExtension.createMemberFunction(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    name: Name,
    returnTypeProvider: (List<FirTypeParameter>) -> ConeKotlinType,
    config: SimpleFunctionBuildingContext.() -> Unit = {}
): FirSimpleFunction {
    val callableId = CallableId(owner.classId, name)
    return SimpleFunctionBuildingContext(session, key, owner, callableId, returnTypeProvider).apply(config).build()
}

/**
 * Creates a top-level function with [returnType] return type
 *
 * Type and value parameters can be configured with [config] builder lambda
 */
public fun FirExtension.createTopLevelFunction(
    key: GeneratedDeclarationKey,
    callableId: CallableId,
    returnType: ConeKotlinType,
    config: SimpleFunctionBuildingContext.() -> Unit = {}
): FirSimpleFunction {
    return createTopLevelFunction(key, callableId, { returnType }, config)
}

/**
 * Creates a top-level function for [owner] class with return type provided by [returnTypeProvider]
 * Use this overload when those types use type parameters of constructed property
 *
 * Type and value parameters can be configured with [config] builder lambda
 */
public fun FirExtension.createTopLevelFunction(
    key: GeneratedDeclarationKey,
    callableId: CallableId,
    returnTypeProvider: (List<FirTypeParameter>) -> ConeKotlinType,
    config: SimpleFunctionBuildingContext.() -> Unit = {}
): FirSimpleFunction {
    require(callableId.classId == null)
    return SimpleFunctionBuildingContext(session, key, owner = null, callableId, returnTypeProvider).apply(config).build()
}
