/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class FirDefaultPropertyAccessor(
    psi: PsiElement?,
    session: FirSession,
    propertyTypeRef: FirTypeRef,
    isGetter: Boolean,
    visibility: Visibility,
    symbol: FirPropertyAccessorSymbol
) : FirPropertyAccessorImpl(psi, session, propertyTypeRef, symbol, isGetter, FirDeclarationStatusImpl(visibility, Modality.FINAL)) {
    override var resolvePhase = FirResolvePhase.BODY_RESOLVE

    final override var body: FirBlock?
        get() = null
        set(_) {}
}

class FirDefaultPropertyGetter(
    psi: PsiElement?,
    session: FirSession,
    propertyTypeRef: FirTypeRef,
    visibility: Visibility,
    symbol: FirPropertyAccessorSymbol = FirPropertyAccessorSymbol()
) : FirDefaultPropertyAccessor(psi, session, propertyTypeRef, isGetter = true, visibility = visibility, symbol = symbol) {
    override val valueParameters: MutableList<FirValueParameter> = mutableListOf()
}

class FirDefaultPropertySetter(
    psi: PsiElement?,
    session: FirSession,
    propertyTypeRef: FirTypeRef,
    visibility: Visibility,
    symbol: FirPropertyAccessorSymbol = FirPropertyAccessorSymbol()
) : FirDefaultPropertyAccessor(psi, session, FirImplicitUnitTypeRef(psi), isGetter = false, visibility = visibility, symbol = symbol) {
    override val valueParameters: MutableList<FirValueParameter> = mutableListOf(
        FirDefaultSetterValueParameter(
            psi,
            session,
            propertyTypeRef,
            FirVariableSymbol(
                CallableId(
                    FqName.ROOT, Name.special("<default-setter-parameter>")
                )
            )
        )
    )
}