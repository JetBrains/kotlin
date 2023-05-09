/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.StandardNames.DEFAULT_VALUE_PARAMETER
import org.jetbrains.kotlin.builtins.StandardNames.ENUM_ENTRIES
import org.jetbrains.kotlin.builtins.StandardNames.ENUM_VALUES
import org.jetbrains.kotlin.builtins.StandardNames.ENUM_VALUE_OF
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.builder.buildEmptyExpressionBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
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
    declarations += buildSimpleFunction {
        source = sourceElement
        this.origin = origin
        this.moduleData = moduleData
        returnTypeRef = buildResolvedTypeRef {
            source = sourceElement
            type = ConeClassLikeTypeImpl(
                StandardClassIds.Array.toLookupTag(),
                arrayOf(
                    ConeClassLikeTypeImpl(this@generateValuesFunction.symbol.toLookupTag(), emptyArray(), isNullable = false)
                ),
                isNullable = false
            )
        }
        name = ENUM_VALUES
        this.status = createStatus(this@generateValuesFunction.status).apply {
            isStatic = true
            isExpect = makeExpect
        }
        symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, ENUM_VALUES))
        resolvePhase = this@generateValuesFunction.resolvePhase
        body = buildEmptyExpressionBlock().also {
            it.replaceTypeRef(returnTypeRef)
        }
    }.apply {
        containingClassForStaticMemberAttr = this@generateValuesFunction.symbol.toLookupTag()
    }
}

fun FirRegularClassBuilder.generateValueOfFunction(
    moduleData: FirModuleData,
    packageFqName: FqName,
    classFqName: FqName,
    makeExpect: Boolean = false,
    origin: FirDeclarationOrigin = FirDeclarationOrigin.Source
) {
    val sourceElement = source?.fakeElement(KtFakeSourceElementKind.EnumGeneratedDeclaration)
    declarations += buildSimpleFunction {
        source = sourceElement
        this.origin = origin
        this.moduleData = moduleData
        returnTypeRef = buildResolvedTypeRef {
            source = sourceElement
            type = ConeClassLikeTypeImpl(
                this@generateValueOfFunction.symbol.toLookupTag(),
                emptyArray(),
                isNullable = false
            )
        }
        name = ENUM_VALUE_OF

        status = createStatus(this@generateValueOfFunction.status).apply {
            isStatic = true
            isExpect = makeExpect
        }
        symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, ENUM_VALUE_OF))
        valueParameters += buildValueParameter vp@{
            source = sourceElement
            containingFunctionSymbol = this@buildSimpleFunction.symbol
            this.origin = origin
            this.moduleData = moduleData
            returnTypeRef = buildResolvedTypeRef {
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
            resolvePhase = this@generateValueOfFunction.resolvePhase
        }
        resolvePhase = this@generateValueOfFunction.resolvePhase
        body = buildEmptyExpressionBlock().also {
            it.replaceTypeRef(returnTypeRef)
        }
    }.apply {
        containingClassForStaticMemberAttr = this@generateValueOfFunction.symbol.toLookupTag()
    }
}

fun FirRegularClassBuilder.generateEntriesGetter(
    moduleData: FirModuleData,
    packageFqName: FqName,
    classFqName: FqName,
    makeExpect: Boolean = false,
    origin: FirDeclarationOrigin = FirDeclarationOrigin.Source
) {
    val sourceElement = source?.fakeElement(KtFakeSourceElementKind.EnumGeneratedDeclaration)
    declarations += buildProperty {
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
                    ConeClassLikeTypeImpl(this@generateEntriesGetter.symbol.toLookupTag(), emptyArray(), isNullable = false)
                ),
                isNullable = false
            )
        }
        name = ENUM_ENTRIES
        this.status = createStatus(this@generateEntriesGetter.status).apply {
            isStatic = true
            isExpect = makeExpect
        }
        symbol = FirPropertySymbol(CallableId(packageFqName, classFqName, ENUM_ENTRIES))
        resolvePhase = this@generateEntriesGetter.resolvePhase
        getter = FirDefaultPropertyGetter(
            sourceElement?.fakeElement(KtFakeSourceElementKind.EnumGeneratedDeclaration),
            moduleData, origin, returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.EnumGeneratedDeclaration),
            Visibilities.Public, symbol, resolvePhase = this@generateEntriesGetter.resolvePhase
        ).apply {
            this.status = createStatus(this@generateEntriesGetter.status).apply {
                isStatic = true
            }
        }
    }.apply {
        containingClassForStaticMemberAttr = this@generateEntriesGetter.symbol.toLookupTag()
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
