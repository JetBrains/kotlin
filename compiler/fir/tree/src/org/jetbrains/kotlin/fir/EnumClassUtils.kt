/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.builder.buildEmptyExpressionBlock
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitStringTypeRef
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

private val ENUM_VALUES = Name.identifier("values")
private val ENUM_VALUE_OF = Name.identifier("valueOf")
private val VALUE = Name.identifier("value")

fun FirRegularClassBuilder.generateValuesFunction(
    moduleData: FirModuleData, packageFqName: FqName, classFqName: FqName, makeExpect: Boolean = false
) {
    val sourceElement = source?.fakeElement(KtFakeSourceElementKind.EnumGeneratedDeclaration)
    declarations += buildSimpleFunction {
        source = sourceElement
        origin = FirDeclarationOrigin.Source
        this.moduleData = moduleData
        returnTypeRef = buildResolvedTypeRef {
            source = sourceElement
            type = ConeClassLikeTypeImpl(
                ConeClassLikeLookupTagImpl(StandardClassIds.Array),
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
        resolvePhase = FirResolvePhase.BODY_RESOLVE
        body = buildEmptyExpressionBlock().also {
            it.replaceTypeRef(returnTypeRef)
        }
    }.apply {
        containingClassForStaticMemberAttr = this@generateValuesFunction.symbol.toLookupTag()
    }
}

fun FirRegularClassBuilder.generateValueOfFunction(
    moduleData: FirModuleData, packageFqName: FqName, classFqName: FqName, makeExpect: Boolean = false
) {
    val sourceElement = source?.fakeElement(KtFakeSourceElementKind.EnumGeneratedDeclaration)
    declarations += buildSimpleFunction {
        source = sourceElement
        origin = FirDeclarationOrigin.Source
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
            origin = FirDeclarationOrigin.Source
            this.moduleData = moduleData
            returnTypeRef = FirImplicitStringTypeRef(source)
            name = VALUE
            this@vp.symbol = FirValueParameterSymbol(VALUE)
            isCrossinline = false
            isNoinline = false
            isVararg = false
            resolvePhase = FirResolvePhase.BODY_RESOLVE
        }
        resolvePhase = FirResolvePhase.BODY_RESOLVE
        body = buildEmptyExpressionBlock().also {
            it.replaceTypeRef(returnTypeRef)
        }
    }.apply {
        containingClassForStaticMemberAttr = this@generateValueOfFunction.symbol.toLookupTag()
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
