/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildReceiverParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.utils.fileNameForPluginGeneratedCallable
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

public class SimpleFunctionBuildingContext(
    session: FirSession,
    key: GeneratedDeclarationKey,
    owner: FirClassSymbol<*>?,
    callableId: CallableId,
    private val returnTypeProvider: (List<FirTypeParameter>) -> ConeKotlinType,
    private val containingFileName: String?,
) : FunctionBuildingContext<FirNamedFunction>(callableId, session, key, owner) {
    private var extensionReceiverTypeProvider: ((List<FirTypeParameter>) -> ConeKotlinType)? = null

    /**
     * Sets [type] as extension receiver type of the function.
     */
    public fun extensionReceiverType(type: ConeKotlinType) {
        extensionReceiverType { type }
    }

    /**
     * Sets type, provided by [typeProvider], as extension receiver type of the function.
     *
     * Use this overload when extension receiver type references type parameters of the function.
     */
    public fun extensionReceiverType(typeProvider: (List<FirTypeParameter>) -> ConeKotlinType) {
        require(extensionReceiverTypeProvider == null) { "Extension receiver type is already initialized" }
        extensionReceiverTypeProvider = typeProvider
    }

    override fun build(): FirNamedFunction {
        return buildSimpleFunction {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = key.origin

            source = getSourceForFirDeclaration()

            symbol = FirNamedFunctionSymbol(callableId)
            name = callableId.callableName

            status = generateStatus()

            dispatchReceiverType = owner?.defaultType()

            this@SimpleFunctionBuildingContext.typeParameters.mapTo(typeParameters) {
                generateTypeParameter(it, symbol)
            }
            initTypeParameterBounds(typeParameters, typeParameters)
            produceContextReceiversTo(contextParameters, typeParameters, origin, symbol)

            this@SimpleFunctionBuildingContext.valueParameters.mapTo(valueParameters) {
                generateValueParameter(it, symbol, typeParameters)
            }
            returnTypeRef = returnTypeProvider(typeParameters).toFirResolvedTypeRef()
            extensionReceiverTypeProvider?.invoke(typeParameters)?.let {
                receiverParameter = buildReceiverParameter {
                    typeRef = it.toFirResolvedTypeRef()
                    symbol = FirReceiverParameterSymbol()
                    moduleData = session.moduleData
                    origin = key.origin
                    containingDeclarationSymbol = this@buildSimpleFunction.symbol
                }
            }
        }.also {
            if (containingFileName != null) {
                require(callableId.classId == null) { "containingFileName could be set only for top-level declarations, but $callableId is a member" }
            }
            it.fileNameForPluginGeneratedCallable = containingFileName
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------

/**
 * Creates a member function for [owner] class with specified [returnType].
 *
 * Type and value parameters can be configured with [config] builder.
 */
public fun FirExtension.createMemberFunction(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    name: Name,
    returnType: ConeKotlinType,
    config: SimpleFunctionBuildingContext.() -> Unit = {}
): FirNamedFunction {
    return createMemberFunction(owner, key, name, { returnType }, config)
}

/**
 * Creates a member function for [owner] class with return type provided by [returnTypeProvider].
 * Use this overload when return type references type parameters of created function.
 *
 * Type and value parameters can be configured with [config] builder.
 */
public fun FirExtension.createMemberFunction(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    name: Name,
    returnTypeProvider: (List<FirTypeParameter>) -> ConeKotlinType,
    config: SimpleFunctionBuildingContext.() -> Unit = {}
): FirNamedFunction {
    val callableId = CallableId(owner.classId, name)
    return SimpleFunctionBuildingContext(session, key, owner, callableId, returnTypeProvider, containingFileName = null)
        .apply(config)
        .apply {
            status {
                isExpect = owner.isExpect
            }
        }.build()
}

/**
 * Creates a top-level function with [callableId] and specified [returnType].
 *
 * Type and value parameters can be configured with [config] builder.
 *
 * @param containingFileName defines the name for a newly created file with this property.
 * The full file path would be `package/of/the/property/containingFileName.kt.
 * If null is passed, then `__GENERATED BUILTINS DECLARATIONS__.kt` would be used
 */
@ExperimentalTopLevelDeclarationsGenerationApi
public fun FirExtension.createTopLevelFunction(
    key: GeneratedDeclarationKey,
    callableId: CallableId,
    returnType: ConeKotlinType,
    containingFileName: String? = null,
    config: SimpleFunctionBuildingContext.() -> Unit = {}
): FirNamedFunction {
    return createTopLevelFunction(key, callableId, { returnType }, containingFileName, config)
}

/**
 * Creates a top-level function with [callableId] and return type provided by [returnTypeProvider].
 * Use this overload when return type references type parameters of the created function.
 *
 * Type and value parameters can be configured with [config] builder.
 *
 * @param containingFileName defines the name for a newly created file with this property.
 * The full file path would be `package/of/the/property/containingFileName.kt.
 * If null is passed, then `__GENERATED BUILTINS DECLARATIONS__.kt` would be used
 */
@ExperimentalTopLevelDeclarationsGenerationApi
public fun FirExtension.createTopLevelFunction(
    key: GeneratedDeclarationKey,
    callableId: CallableId,
    returnTypeProvider: (List<FirTypeParameter>) -> ConeKotlinType,
    containingFileName: String? = null,
    config: SimpleFunctionBuildingContext.() -> Unit = {}
): FirNamedFunction {
    require(callableId.classId == null)
    return SimpleFunctionBuildingContext(
        session, key, owner = null, callableId,
        returnTypeProvider, containingFileName
    ).apply(config).build()
}
