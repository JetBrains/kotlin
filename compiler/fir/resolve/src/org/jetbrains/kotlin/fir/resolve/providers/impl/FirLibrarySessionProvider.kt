/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirLibrarySessionProvider(
    override val symbolProvider: FirSymbolProvider
) : FirProvider() {
    override fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration<*>? =
        symbolProvider.getClassLikeSymbolByFqName(classId)?.fir

    override fun getFirClassifierContainerFile(fqName: ClassId): FirFile = shouldNotBeCalled()

    override fun getFirClassifierContainerFileIfAny(fqName: ClassId): FirFile? = null
    override fun getFirCallableContainerFile(symbol: FirCallableSymbol<*>): FirFile? = null
    override fun getFirFilesByPackage(fqName: FqName): List<FirFile> = emptyList()

    @FirProviderInternals
    override fun recordGeneratedClass(owner: FirAnnotatedDeclaration, klass: FirRegularClass) = shouldNotBeCalled()

    @FirProviderInternals
    override fun recordGeneratedMember(owner: FirAnnotatedDeclaration, klass: FirDeclaration) = shouldNotBeCalled()

    override fun getClassNamesInPackage(fqName: FqName): Set<Name> = shouldNotBeCalled()

    private fun shouldNotBeCalled(): Nothing = error("Should not be called for FirLibrarySessionProvider")
}