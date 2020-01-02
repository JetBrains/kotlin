/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirSimpleFunctionImpl
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.Name

class FirJavaMethod(
    session: FirSession,
    source: FirSourceElement?,
    symbol: FirNamedFunctionSymbol,
    name: Name,
    visibility: Visibility,
    modality: Modality?,
    returnTypeRef: FirJavaTypeRef,
    isStatic: Boolean
) : FirSimpleFunctionImpl(
    source,
    session,
    returnTypeRef,
    null,
    name,
    FirDeclarationStatusImpl(visibility, modality).apply {
        this.isStatic = isStatic
        isExpect = false
        isActual = false
        isOverride = false
        isOperator = true // All Java methods with name that allows to use it in operator form are considered operators
        isInfix = false
        isInline = false
        isTailRec = false
        isExternal = false
        isSuspend = false
    },
    symbol
) {
    init {
        resolvePhase = FirResolvePhase.DECLARATIONS
    }
}