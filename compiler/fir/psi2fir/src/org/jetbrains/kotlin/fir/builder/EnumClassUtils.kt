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
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private val ENUM_VALUES = Name.identifier("values")

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
                source, ConeClassTypeImpl(
                    ConeClassLikeLookupTagImpl(StandardClassIds.Array),
                    arrayOf(
                        ConeClassTypeImpl(ConeClassLikeLookupTagImpl(this.symbol.classId), emptyArray(), isNullable = false)
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