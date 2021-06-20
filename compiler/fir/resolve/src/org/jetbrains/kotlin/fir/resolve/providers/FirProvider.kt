/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers

import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@RequiresOptIn
annotation class FirProviderInternals

abstract class FirProvider : FirSessionComponent {
    /**
     * [symbolProvider] for [FirProvider] may provide only symbols from sources of current module
     */
    abstract val symbolProvider: FirSymbolProvider

    open val isPhasedFirAllowed: Boolean get() = false

    abstract fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration<*>?

    abstract fun getFirClassifierContainerFile(fqName: ClassId): FirFile

    abstract fun getFirClassifierContainerFileIfAny(fqName: ClassId): FirFile?

    open fun getFirClassifierContainerFile(symbol: FirClassLikeSymbol<*>): FirFile =
        getFirClassifierContainerFile(symbol.classId)

    open fun getFirClassifierContainerFileIfAny(symbol: FirClassLikeSymbol<*>): FirFile? =
        getFirClassifierContainerFileIfAny(symbol.classId)

    abstract fun getFirCallableContainerFile(symbol: FirCallableSymbol<*>): FirFile?

    abstract fun getFirFilesByPackage(fqName: FqName): List<FirFile>

    @FirProviderInternals
    abstract fun recordGeneratedClass(owner: FirAnnotatedDeclaration<*>, klass: FirRegularClass)

    @FirProviderInternals
    abstract fun recordGeneratedMember(owner: FirAnnotatedDeclaration<*>, klass: FirDeclaration<*>)

    abstract fun getClassNamesInPackage(fqName: FqName): Set<Name>
}
