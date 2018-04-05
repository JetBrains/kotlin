/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class FirCompositeSymbolProvider(val providers: List<FirSymbolProvider>) : FirSymbolProvider {

    override fun getPackage(fqName: FqName): FqName? {
        return providers.firstNotNullResult { it.getPackage(fqName) }
    }

    override fun getSymbolByFqName(classId: ClassId): ConeSymbol? {
        return providers.firstNotNullResult { it.getSymbolByFqName(classId) }
    }
}