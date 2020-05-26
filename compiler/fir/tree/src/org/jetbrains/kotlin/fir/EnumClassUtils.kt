/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.builder.buildEmptyExpressionBlock
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitStringTypeRef
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private val ENUM_VALUES = Name.identifier("values")
private val ENUM_VALUE_OF = Name.identifier("valueOf")
private val VALUE = Name.identifier("value")

fun FirRegularClassBuilder.generateValuesFunction(session: FirSession, packageFqName: FqName, classFqName: FqName) {
    declarations += buildSimpleFunction {
        source = this@generateValuesFunction.source
        origin = FirDeclarationOrigin.Source
        this.session = session
        returnTypeRef = buildResolvedTypeRef {
            source = this@generateValuesFunction.source
            type = ConeClassLikeTypeImpl(
                ConeClassLikeLookupTagImpl(StandardClassIds.Array),
                arrayOf(
                    ConeClassLikeTypeImpl(this@generateValuesFunction.symbol.toLookupTag(), emptyArray(), isNullable = false)
                ),
                isNullable = false
            )
        }
        name = ENUM_VALUES
        this.status = FirDeclarationStatusImpl(Visibilities.PUBLIC, Modality.FINAL).apply {
            isStatic = true
        }
        symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, ENUM_VALUES))
        resolvePhase = FirResolvePhase.BODY_RESOLVE
        body = buildEmptyExpressionBlock()
    }
}

fun FirRegularClassBuilder.generateValueOfFunction(session: FirSession, packageFqName: FqName, classFqName: FqName) {
    declarations += buildSimpleFunction {
        source = this@generateValueOfFunction.source
        origin = FirDeclarationOrigin.Source
        this.session = session
        returnTypeRef = buildResolvedTypeRef {
            source = this@generateValueOfFunction.source
            type = ConeClassLikeTypeImpl(
                this@generateValueOfFunction.symbol.toLookupTag(),
                emptyArray(),
                isNullable = false
            )
        }
        name = ENUM_VALUE_OF
        status = FirDeclarationStatusImpl(Visibilities.PUBLIC, Modality.FINAL).apply {
            isStatic = true
        }
        symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, ENUM_VALUE_OF))
        valueParameters += buildValueParameter vp@{
            source = this@generateValueOfFunction.source
            origin = FirDeclarationOrigin.Source
            this@vp.session = session
            returnTypeRef = FirImplicitStringTypeRef(source)
            name = VALUE
            this@vp.symbol = FirVariableSymbol(VALUE)
            isCrossinline = false
            isNoinline = false
            isVararg = false
        }
        resolvePhase = FirResolvePhase.BODY_RESOLVE
        body = buildEmptyExpressionBlock()
    }
}
