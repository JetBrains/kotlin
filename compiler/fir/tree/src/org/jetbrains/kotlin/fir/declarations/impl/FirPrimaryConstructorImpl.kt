/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef

class FirPrimaryConstructorImpl(
    source: FirSourceElement?,
    session: FirSession,
    returnTypeRef: FirTypeRef,
    receiverTypeRef: FirTypeRef?,
    status: FirDeclarationStatus,
    symbol: FirConstructorSymbol
) : FirConstructorImpl(source, session, returnTypeRef, receiverTypeRef, status, symbol) {
    override val isPrimary: Boolean get() = true
}