/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildReceiverParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.toFirResolvedTypeRef
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

public class PropertyBuildingContext(
    session: FirSession,
    key: GeneratedDeclarationKey,
    owner: FirClassSymbol<*>?,
    private val callableId: CallableId,
    private val returnTypeProvider: (List<FirTypeParameterRef>) -> ConeKotlinType,
    private val isVal: Boolean,
    private val hasBackingField: Boolean,
) : DeclarationBuildingContext<FirProperty>(session, key, owner) {
    private var setterVisibility: Visibility? = null
    private var extensionReceiverTypeProvider: ((List<FirTypeParameter>) -> ConeKotlinType)? = null

    /**
     * Sets [type] as extension receiver type of constructed property
     */
    public fun extensionReceiverType(type: ConeKotlinType) {
        extensionReceiverType { type }
    }

    /**
     * Sets type, provided by [typeProvider], as extension receiver type of constructed property
     *
     * Use this overload when extension receiver type uses type parameters of constructed property
     */
    public fun extensionReceiverType(typeProvider: (List<FirTypeParameter>) -> ConeKotlinType) {
        extensionReceiverTypeProvider = typeProvider
    }

    /**
     * Declares [visibility] of property setter if property marked as var
     * If this function is not called then setter will have same visibility
     *   as property itself
     */
    public fun setter(visibility: Visibility) {
        setterVisibility = visibility
    }

    override fun build(): FirProperty {
        return buildProperty {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = key.origin

            symbol = FirPropertySymbol(callableId)
            name = callableId.callableName

            val resolvedStatus = generateStatus()
            status = resolvedStatus

            dispatchReceiverType = owner?.defaultType()

            this@PropertyBuildingContext.typeParameters.mapTo(typeParameters) {
                generateTypeParameter(it, symbol)
            }
            initTypeParameterBounds(typeParameters, typeParameters)

            returnTypeRef = returnTypeProvider.invoke(typeParameters).toFirResolvedTypeRef()
            extensionReceiverTypeProvider?.invoke(typeParameters)?.let {
                receiverParameter = buildReceiverParameter {
                    typeRef = it.toFirResolvedTypeRef()
                }
            }

            produceContextReceiversTo(contextReceivers, typeParameters)

            isVar = !isVal
            getter = FirDefaultPropertyGetter(
                source = null, session.moduleData, key.origin, returnTypeRef, status.visibility, symbol,
                Modality.FINAL, resolvedStatus.effectiveVisibility
            )
            if (isVar) {
                setter = FirDefaultPropertySetter(
                    source = null, session.moduleData, key.origin, returnTypeRef, setterVisibility ?: status.visibility,
                    symbol, Modality.FINAL, resolvedStatus.effectiveVisibility
                )
            } else {
                require(setterVisibility == null) { "isVar = false but setterVisibility is specified. Did you forget to set isVar = true?" }
            }
            if (hasBackingField) {
                backingField = FirDefaultPropertyBackingField(session.moduleData, mutableListOf(), returnTypeRef, isVar, symbol, status)
            }
            isLocal = false
            bodyResolveState = FirPropertyBodyResolveState.EVERYTHING_RESOLVED
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------

/**
 * Creates a member property for [owner] class with [returnType] return type
 */
public fun FirExtension.createMemberProperty(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    name: Name,
    returnType: ConeKotlinType,
    isVal: Boolean = true,
    hasBackingField: Boolean = true,
    config: PropertyBuildingContext.() -> Unit = {}
): FirProperty {
    return createMemberProperty(owner, key, name, { returnType }, isVal, hasBackingField, config)
}

/**
 * Creates a member property for [owner] class with return type provided by [returnTypeProvider]
 * Use this overload when those types use type parameters of constructed property
 */
public fun FirExtension.createMemberProperty(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    name: Name,
    returnTypeProvider: (List<FirTypeParameterRef>) -> ConeKotlinType,
    isVal: Boolean = true,
    hasBackingField: Boolean = true,
    config: PropertyBuildingContext.() -> Unit = {}
): FirProperty {
    val callableId = CallableId(owner.classId, name)
    return PropertyBuildingContext(session, key, owner, callableId, returnTypeProvider, isVal, hasBackingField).apply(config).build()
}

/**
 * Creates a top-level property class with [returnType] return type
 *
 * If you create top-level extension property don't forget to set [hasBackingField] to false,
 *   since such properties never have backing fields
 */
public fun FirExtension.createTopLevelProperty(
    key: GeneratedDeclarationKey,
    callableId: CallableId,
    returnType: ConeKotlinType,
    isVal: Boolean = true,
    hasBackingField: Boolean = true,
    config: PropertyBuildingContext.() -> Unit = {}
): FirProperty {
    return createTopLevelProperty(key, callableId, { returnType }, isVal, hasBackingField, config)
}

/**
 * Creates a top-level property with return type provided by [returnTypeProvider]
 *
 * If you create top-level extension property don't forget to set [hasBackingField] to false,
 *   since such properties never have backing fields
 *
 * Use this overload when those types use type parameters of constructed property
 */
public fun FirExtension.createTopLevelProperty(
    key: GeneratedDeclarationKey,
    callableId: CallableId,
    returnTypeProvider: (List<FirTypeParameterRef>) -> ConeKotlinType,
    isVal: Boolean = true,
    hasBackingField: Boolean = true,
    config: PropertyBuildingContext.() -> Unit = {}
): FirProperty {
    require(callableId.classId == null)
    return PropertyBuildingContext(session, key, owner = null, callableId, returnTypeProvider, isVal, hasBackingField).apply(config).build()
}
