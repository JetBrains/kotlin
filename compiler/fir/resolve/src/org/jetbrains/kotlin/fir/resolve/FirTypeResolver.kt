/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.types.ConeKotlinType

interface FirTypeResolver {

    fun resolveType(type: FirType, scope: FirScope): ConeKotlinType

    companion object {
        fun getInstance(session: FirSession): FirTypeResolver = session.service()
    }
}