/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.ConeClassifierSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirSelfImportingScope(val fqName: FqName, val session: FirSession) : FirScope {

    private val symbolProvider = FirSymbolProvider.getInstance(session)

    private val cache = ContainerUtil.newConcurrentMap<Name, ConeClassifierSymbol>()
    private val absentKeys = ContainerUtil.newConcurrentSet<Name>()

    override fun processClassifiersByName(
        name: Name,
        position: FirPosition,
        processor: (ConeClassifierSymbol) -> Boolean
    ): Boolean {


        if (name in absentKeys) return true
        val symbol = cache[name] ?: run {
            val unambiguousFqName = ClassId(fqName, name)
            val computed = symbolProvider.getClassLikeSymbolByFqName(unambiguousFqName)
            if (computed == null) {
                absentKeys += name
            } else {
                cache[name] = computed
            }
            computed
        }

        return if (symbol != null) {
            processor(symbol)
        } else {
            true
        }
    }
}
