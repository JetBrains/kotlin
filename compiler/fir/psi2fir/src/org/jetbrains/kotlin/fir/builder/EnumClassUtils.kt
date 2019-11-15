/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.addDeclaration
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirModifiableRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirSimpleFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitStringTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private val ENUM_VALUES = Name.identifier("values")
private val ENUM_VALUE_OF = Name.identifier("valueOf")
private val VALUE = Name.identifier("value")

fun FirModifiableRegularClass.generateValuesFunction(
    session: FirSession, packageFqName: FqName, classFqName: FqName
) {
    val symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, ENUM_VALUES))
    val status = FirDeclarationStatusImpl(Visibilities.PUBLIC, Modality.FINAL).apply {
        isStatic = true
    }
    addDeclaration(
        FirSimpleFunctionImpl(
            source, session,
            FirResolvedTypeRefImpl(
                source, ConeClassLikeTypeImpl(
                    ConeClassLikeLookupTagImpl(StandardClassIds.Array),
                    arrayOf(
                        ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(this.symbol.classId), emptyArray(), isNullable = false)
                    ),
                    isNullable = false
                )
            ),
            null, ENUM_VALUES, status, symbol
        ).apply {
            body = FirEmptyExpressionBlock()
        }
    )
}

fun FirModifiableRegularClass.generateValueOfFunction(
    session: FirSession, packageFqName: FqName, classFqName: FqName
) {
    val symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, ENUM_VALUE_OF))
    val status = FirDeclarationStatusImpl(Visibilities.PUBLIC, Modality.FINAL).apply {
        isStatic = true
    }
    addDeclaration(
        FirSimpleFunctionImpl(
            source, session,
            FirResolvedTypeRefImpl(
                source, ConeClassLikeTypeImpl(
                    ConeClassLikeLookupTagImpl(this.symbol.classId),
                    emptyArray(),
                    isNullable = false
                )
            ),
            null, ENUM_VALUE_OF, status, symbol
        ).apply {
            valueParameters += FirValueParameterImpl(
                source, session, FirImplicitStringTypeRef(source),
                VALUE, FirVariableSymbol(VALUE),
                defaultValue = null, isCrossinline = false, isNoinline = false, isVararg = false
            )
            body = FirEmptyExpressionBlock()
        }
    )
}
