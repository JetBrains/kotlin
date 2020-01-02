/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

class FirJavaValueParameter(
    session: FirSession,
    source: FirSourceElement?,
    name: Name,
    returnTypeRef: FirJavaTypeRef,
    isVararg: Boolean
) : FirValueParameterImpl(
    source,
    session,
    returnTypeRef,
    name,
    FirVariableSymbol(name),
    defaultValue = null,
    isCrossinline = false,
    isNoinline = false,
    isVararg = isVararg
) {
    init {
        resolvePhase = FirResolvePhase.DECLARATIONS
    }
}