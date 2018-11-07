/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirNamedDeclaration
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

interface FirProvider : FirSymbolProvider {
    fun getFirClassifierByFqName(fqName: ClassId): FirMemberDeclaration?

    override fun getSymbolByFqName(classId: ClassId): ConeSymbol?

    override fun getPackage(fqName: FqName): FqName? {
        if (getFirFilesByPackage(fqName).isNotEmpty()) return fqName
        return null
    }

    fun getFirClassifierContainerFile(fqName: ClassId): FirFile

    fun getFirClassifierBySymbol(symbol: ConeSymbol): FirNamedDeclaration?

    companion object {
        fun getInstance(session: FirSession): FirProvider = session.service()
    }

    fun getFirFilesByPackage(fqName: FqName): List<FirFile>
}