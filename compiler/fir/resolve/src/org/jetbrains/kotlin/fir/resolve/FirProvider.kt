/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class FirProvider : FirSymbolProvider() {
    abstract fun getFirClassifierByFqName(fqName: ClassId): FirClassLikeDeclaration<*>?

    abstract override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>?

    abstract override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>>

    override fun getPackage(fqName: FqName): FqName? {
        if (getFirFilesByPackage(fqName).isNotEmpty()) return fqName
        return null
    }

    abstract fun getFirClassifierContainerFile(fqName: ClassId): FirFile

    fun getFirClassifierContainerFile(symbol: FirClassLikeSymbol<*>): FirFile =
        getFirClassifierContainerFile(symbol.classId)

    abstract fun getFirCallableContainerFile(symbol: FirCallableSymbol<*>): FirFile?

    companion object {
        fun getInstance(session: FirSession): FirProvider = session.service()
    }

    abstract fun getFirFilesByPackage(fqName: FqName): List<FirFile>
}
