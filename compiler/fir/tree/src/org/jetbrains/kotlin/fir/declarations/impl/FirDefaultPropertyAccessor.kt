/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildDefaultSetterValueParameter
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@OptIn(FirImplementationDetail::class)
abstract class FirDefaultPropertyAccessor(
    source: FirSourceElement?,
    session: FirSession,
    origin: FirDeclarationOrigin,
    propertyTypeRef: FirTypeRef,
    valueParameters: MutableList<FirValueParameter>,
    isGetter: Boolean,
    visibility: Visibility,
    symbol: FirPropertyAccessorSymbol
) : FirPropertyAccessorImpl(
    source,
    session,
    resolvePhase = FirResolvePhase.BODY_RESOLVE,
    origin,
    FirDeclarationAttributes(),
    propertyTypeRef,
    valueParameters,
    body = null,
    FirDeclarationStatusImpl(visibility, Modality.FINAL),
    containerSource = null,
    dispatchReceiverType = null,
    contractDescription = FirEmptyContractDescription,
    symbol,
    isGetter,
    annotations = mutableListOf(),
    typeParameters = mutableListOf(),
) {
    override var resolvePhase
        get() = FirResolvePhase.BODY_RESOLVE
        set(_) {}

    final override var body: FirBlock?
        get() = null
        set(_) {}

    companion object {
        fun createGetterOrSetter(
            source: FirSourceElement?,
            session: FirSession,
            origin: FirDeclarationOrigin,
            propertyTypeRef: FirTypeRef,
            visibility: Visibility,
            isGetter: Boolean
        ): FirDefaultPropertyAccessor {
            return if (isGetter) {
                FirDefaultPropertyGetter(source, session, origin, propertyTypeRef, visibility)
            } else {
                FirDefaultPropertySetter(source, session, origin, propertyTypeRef, visibility)
            }
        }
    }
}

class FirDefaultPropertyGetter(
    source: FirSourceElement?,
    session: FirSession,
    origin: FirDeclarationOrigin,
    propertyTypeRef: FirTypeRef,
    visibility: Visibility,
    symbol: FirPropertyAccessorSymbol = FirPropertyAccessorSymbol()
) : FirDefaultPropertyAccessor(
    source,
    session,
    origin,
    propertyTypeRef,
    valueParameters = mutableListOf(),
    isGetter = true,
    visibility = visibility,
    symbol = symbol
)

class FirDefaultPropertySetter(
    source: FirSourceElement?,
    session: FirSession,
    origin: FirDeclarationOrigin,
    propertyTypeRef: FirTypeRef,
    visibility: Visibility,
    symbol: FirPropertyAccessorSymbol = FirPropertyAccessorSymbol()
) : FirDefaultPropertyAccessor(
    source,
    session,
    origin,
    FirImplicitUnitTypeRef(source),
    valueParameters = mutableListOf(
        buildDefaultSetterValueParameter builder@{
            this@builder.source = source
            this@builder.session = session
            this@builder.origin = origin
            this@builder.returnTypeRef = propertyTypeRef
            this@builder.symbol = FirVariableSymbol(CallableId(FqName.ROOT, Name.special("<default-setter-parameter>")))
        }
    ),
    isGetter = false,
    visibility,
    symbol
)
