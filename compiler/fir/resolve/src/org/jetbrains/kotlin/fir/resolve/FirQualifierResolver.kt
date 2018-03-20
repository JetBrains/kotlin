/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.UnambiguousFqName
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirQualifierPart
import org.jetbrains.kotlin.name.FqName

interface FirQualifierResolver {
    fun resolveTypeWithPrefix(parts: List<FirQualifierPart>, prefix: UnambiguousFqName): ConeKotlinType?

    fun resolveType(parts: List<FirQualifierPart>): ConeKotlinType?

    companion object {
        fun getInstance(session: FirSession): FirQualifierResolver = session.service()
    }
}

