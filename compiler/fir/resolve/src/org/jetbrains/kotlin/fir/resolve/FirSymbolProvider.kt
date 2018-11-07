/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

interface FirSymbolProvider {

    fun getSymbolByFqName(classId: ClassId): ConeSymbol?

    fun getPackage(fqName: FqName): FqName? // TODO: Replace to symbol sometime

    companion object {
        fun getInstance(session: FirSession) = session.service<FirSymbolProvider>()
    }
}