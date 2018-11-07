/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirUserType

interface FirTypeResolver {

    fun resolveType(type: FirType, scope: FirScope, position: FirPosition): ConeKotlinType
    fun resolveToSymbol(type: FirType, scope: FirScope, position: FirPosition): ConeSymbol?

    companion object {
        fun getInstance(session: FirSession): FirTypeResolver = session.service()
    }

    fun resolveUserType(type: FirUserType, symbol: ConeSymbol?, scope: FirScope): ConeKotlinType
}