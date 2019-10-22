/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef

interface FirTypeResolver {

    fun resolveType(typeRef: FirTypeRef, scope: FirScope): ConeKotlinType
    fun resolveToSymbol(typeRef: FirTypeRef, scope: FirScope): FirClassifierSymbol<*>?

    companion object {
        fun getInstance(session: FirSession): FirTypeResolver = session.service()
    }

    fun resolveUserType(typeRef: FirUserTypeRef, symbol: FirClassifierSymbol<*>?, scope: FirScope): ConeKotlinType
}
