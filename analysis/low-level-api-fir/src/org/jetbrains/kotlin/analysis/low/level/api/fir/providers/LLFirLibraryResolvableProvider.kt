/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLibraryResolveModuleFileCache
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class LLFirLibraryResolvableProvider(
    override val symbolProvider: JvmClassFileBasedSymbolProvider,
    private val cache: LLFirLibraryResolveModuleFileCache,
) : FirProvider() {

    override fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration? =
        symbolProvider.getClassLikeSymbolByClassId(classId)?.fir

    override fun getFirClassifierContainerFile(fqName: ClassId): FirFile =
        getFirClassifierContainerFileIfAny(fqName)!!

    override fun getFirClassifierContainerFileIfAny(fqName: ClassId): FirFile? = null
    override fun getFirCallableContainerFile(symbol: FirCallableSymbol<*>): FirFile? = null
    override fun getFirFilesByPackage(fqName: FqName): List<FirFile> = emptyList()

    override fun getClassNamesInPackage(fqName: FqName): Set<Name> = shouldNotBeCalled()

    private fun shouldNotBeCalled(): Nothing = error("Should not be called for LLFirLibrariesSessionProvider")
}
