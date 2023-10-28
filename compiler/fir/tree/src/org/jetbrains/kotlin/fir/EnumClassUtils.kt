/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.StandardNames.DEFAULT_VALUE_PARAMETER
import org.jetbrains.kotlin.builtins.StandardNames.ENUM_ENTRIES
import org.jetbrains.kotlin.builtins.StandardNames.ENUM_VALUES
import org.jetbrains.kotlin.builtins.StandardNames.ENUM_VALUE_OF
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.builder.buildEmptyExpressionBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds

fun FirRegularClassBuilder.generateValuesFunction(
    moduleData: FirModuleData,
    packageFqName: FqName,
    classFqName: FqName,
    makeExpect: Boolean = false,
    origin: FirDeclarationOrigin = FirDeclarationOrigin.Source
) {
    val sourceElement = source?.fakeElement(KtFakeSourceElementKind.EnumGeneratedDeclaration)

    declarations += createEnumValuesFunction(
        symbol,
        status,
        resolvePhase,
        moduleData,
        packageFqName,
        classFqName,
        origin,
        makeExpect,
        sourceElement
    )
}

fun FirRegularClassBuilder.generateValueOfFunction(
    moduleData: FirModuleData,
    packageFqName: FqName,
    classFqName: FqName,
    makeExpect: Boolean = false,
    origin: FirDeclarationOrigin = FirDeclarationOrigin.Source
) {
    val sourceElement = source?.fakeElement(KtFakeSourceElementKind.EnumGeneratedDeclaration)

    declarations += createEnumValueOfFunction(
        symbol,
        status,
        resolvePhase,
        moduleData,
        packageFqName,
        classFqName,
        origin,
        makeExpect,
        sourceElement
    )
}

fun FirRegularClassBuilder.generateEntriesGetter(
    moduleData: FirModuleData,
    packageFqName: FqName,
    classFqName: FqName,
    makeExpect: Boolean = false,
    origin: FirDeclarationOrigin = FirDeclarationOrigin.Source
) {
    val sourceElement = source?.fakeElement(KtFakeSourceElementKind.EnumGeneratedDeclaration)
    declarations += createEnumEntriesGetter(
        symbol,
        status,
        resolvePhase,
        moduleData,
        packageFqName,
        classFqName,
        origin,
        makeExpect,
        sourceElement
    )
}

fun createEnumValuesFunction(
    owner: FirClassSymbol<*>,
    ownerStatus: FirDeclarationStatus,
    resolvePhase: FirResolvePhase,
    moduleData: FirModuleData,
    packageFqName: FqName,
    classFqName: FqName,
    origin: FirDeclarationOrigin,
    makeExpect: Boolean = false,
    sourceElement: KtSourceElement? = null
): FirSimpleFunction {
    return buildSimpleFunction {
        source = sourceElement
        this.origin = origin
        this.moduleData = moduleData
        val returnTypeRef = buildResolvedTypeRef {
            source = sourceElement
            type = ConeClassLikeTypeImpl(
                StandardClassIds.Array.toLookupTag(),
                arrayOf(
                    ConeClassLikeTypeImpl(
                        owner.toLookupTag(),
                        ConeTypeProjection.EMPTY_ARRAY,
                        isNullable = false
                    )
                ),
                isNullable = false
            )
        }
        this.returnTypeRef = returnTypeRef
        name = ENUM_VALUES
        this.status = createStatus(ownerStatus).apply {
            isStatic = true
            isExpect = makeExpect
        }
        symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, ENUM_VALUES))
        this.resolvePhase = resolvePhase
        body = buildEmptyExpressionBlock().also {
            it.replaceConeTypeOrNull(returnTypeRef.type)
        }
    }.apply {
        containingClassForStaticMemberAttr = owner.toLookupTag()
    }
}

fun createEnumValueOfFunction(
    owner: FirClassSymbol<*>,
    ownerStatus: FirDeclarationStatus,
    resolvePhase: FirResolvePhase,
    moduleData: FirModuleData,
    packageFqName: FqName,
    classFqName: FqName,
    origin: FirDeclarationOrigin,
    makeExpect: Boolean = false,
    sourceElement: KtSourceElement? = null
): FirSimpleFunction {
    return buildSimpleFunction {
        source = sourceElement
        this.origin = origin
        this.moduleData = moduleData
        val returnTypeRef = buildResolvedTypeRef {
            source = sourceElement
            type = ConeClassLikeTypeImpl(
                owner.toLookupTag(),
                emptyArray(),
                isNullable = false
            )
        }
        this.returnTypeRef = returnTypeRef
        name = ENUM_VALUE_OF

        status = createStatus(ownerStatus).apply {
            isStatic = true
            isExpect = makeExpect
        }
        symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, ENUM_VALUE_OF))
        valueParameters += buildValueParameter vp@{
            source = sourceElement
            containingFunctionSymbol = this@buildSimpleFunction.symbol
            this.origin = origin
            this.moduleData = moduleData
            this.returnTypeRef = buildResolvedTypeRef {
                source = sourceElement
                type = ConeClassLikeTypeImpl(
                    StandardClassIds.String.toLookupTag(),
                    emptyArray(),
                    isNullable = false
                )
            }
            name = DEFAULT_VALUE_PARAMETER
            this@vp.symbol = FirValueParameterSymbol(DEFAULT_VALUE_PARAMETER)
            isCrossinline = false
            isNoinline = false
            isVararg = false
            this.resolvePhase = resolvePhase
        }
        this.resolvePhase = resolvePhase
        body = buildEmptyExpressionBlock().also {
            it.replaceConeTypeOrNull(returnTypeRef.type)
        }
    }.apply {
        containingClassForStaticMemberAttr = owner.toLookupTag()
    }
}

fun createEnumEntriesGetter(
    owner: FirClassSymbol<*>,
    ownerStatus: FirDeclarationStatus,
    resolvePhase: FirResolvePhase,
    moduleData: FirModuleData,
    packageFqName: FqName,
    classFqName: FqName,
    origin: FirDeclarationOrigin,
    makeExpect: Boolean = false,
    sourceElement: KtSourceElement? = null
): FirProperty {
    return buildProperty {
        source = sourceElement
        isVar = false
        isLocal = false
        this.origin = origin
        this.moduleData = moduleData
        returnTypeRef = buildResolvedTypeRef {
            source = sourceElement
            type = ConeClassLikeTypeImpl(
                StandardClassIds.EnumEntries.toLookupTag(),
                arrayOf(
                    ConeClassLikeTypeImpl(
                        owner.toLookupTag(),
                        ConeTypeProjection.EMPTY_ARRAY,
                        isNullable = false
                    )
                ),
                isNullable = false
            )
        }
        name = ENUM_ENTRIES
        this.status = createStatus(ownerStatus).apply {
            isStatic = true
            isExpect = makeExpect
        }
        symbol = FirPropertySymbol(CallableId(packageFqName, classFqName, ENUM_ENTRIES))
        this.resolvePhase = resolvePhase
        getter = FirDefaultPropertyGetter(
            sourceElement?.fakeElement(KtFakeSourceElementKind.EnumGeneratedDeclaration),
            moduleData, origin, returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.EnumGeneratedDeclaration),
            Visibilities.Public, symbol, resolvePhase = resolvePhase
        ).apply {
            this.status = createStatus(ownerStatus).apply {
                isStatic = true
            }
        }
    }.apply {
        containingClassForStaticMemberAttr = owner.toLookupTag()
    }
}

private fun createStatus(parentStatus: FirDeclarationStatus): FirDeclarationStatusImpl {
    val parentEffectiveVisibility = (parentStatus as? FirResolvedDeclarationStatusImpl)?.effectiveVisibility
    return if (parentEffectiveVisibility != null) {
        FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.FINAL, parentEffectiveVisibility)
    } else {
        FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL)
    }
}
