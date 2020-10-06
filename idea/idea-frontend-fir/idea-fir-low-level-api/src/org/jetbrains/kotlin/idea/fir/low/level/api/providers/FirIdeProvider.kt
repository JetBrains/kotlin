/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.providers

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.dependenciesWithoutSelf
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.KotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.IndexHelper
import org.jetbrains.kotlin.idea.fir.low.level.api.PackageExistenceCheckerForMultipleModules
import org.jetbrains.kotlin.idea.fir.low.level.api.PackageExistenceCheckerForSingleModule
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.util.collectTransitiveDependenciesWithSelf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

@ThreadSafeMutableState
internal class FirIdeProvider(
    project: Project,
    val session: FirSession,
    moduleInfo: ModuleSourceInfo,
    private val kotlinScopeProvider: KotlinScopeProvider,
    firFileBuilder: FirFileBuilder,
    val cache: ModuleFileCache,
    searchScope: GlobalSearchScope,
) : FirProvider() {
    override val symbolProvider: FirSymbolProvider = SymbolProvider()

    private val indexHelper = IndexHelper(project, searchScope)
    private val packageExistenceChecker = PackageExistenceCheckerForSingleModule(project, moduleInfo)

    private val providerHelper = FirProviderHelper(
        cache,
        firFileBuilder,
        indexHelper,
        packageExistenceChecker,
    )

    override val isPhasedFirAllowed: Boolean get() = true

    override fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration<*>? =
        providerHelper.getFirClassifierByFqName(classId)

    override fun getFirClassifierContainerFile(fqName: ClassId): FirFile {
        return getFirClassifierContainerFileIfAny(fqName)
            ?: error("Couldn't find container for ${fqName}")
    }

    override fun getFirClassifierContainerFileIfAny(fqName: ClassId): FirFile? {
        val fir = getFirClassifierByFqName(fqName) ?: return null // Necessary to ensure cacheProvider contains this classifier
        return cache.getContainerFirFile(fir)
    }

    override fun getFirClassifierContainerFile(symbol: FirClassLikeSymbol<*>): FirFile {
        return getFirClassifierContainerFileIfAny(symbol)
            ?: error("Couldn't find container for ${symbol.classId}")
    }

    override fun getFirClassifierContainerFileIfAny(symbol: FirClassLikeSymbol<*>): FirFile? =
        cache.getContainerFirFile(symbol.fir)


    override fun getFirCallableContainerFile(symbol: FirCallableSymbol<*>): FirFile? {
        symbol.overriddenSymbol?.let {
            return getFirCallableContainerFile(it)
        }
        if (symbol is FirAccessorSymbol) {
            val fir = symbol.fir
            if (fir is FirSyntheticProperty) {
                return getFirCallableContainerFile(fir.getter.delegate.symbol)
            }
        }
        return cache.getContainerFirFile(symbol.fir)
    }

    override fun getFirFilesByPackage(fqName: FqName): List<FirFile> = error("Should not be called in FIR IDE")


    // TODO move out of here
    // used only for completion
    fun buildFunctionWithBody(ktNamedFunction: KtNamedFunction): FirFunction<*> {
        return RawFirBuilder(session, kotlinScopeProvider, stubMode = false).buildFunctionWithBody(ktNamedFunction)
    }

    fun buildPropertyWithBody(ktNamedFunction: KtProperty): FirProperty {
        return RawFirBuilder(session, kotlinScopeProvider, stubMode = false).buildPropertyWithBody(ktNamedFunction)
    }


    @FirProviderInternals
    override fun recordGeneratedClass(owner: FirAnnotatedDeclaration, klass: FirRegularClass) {
        TODO()
    }

    @FirProviderInternals
    override fun recordGeneratedMember(owner: FirAnnotatedDeclaration, klass: FirDeclaration) {
        TODO()
    }

    override fun getClassNamesInPackage(fqName: FqName): Set<Name> {
        // TODO: KT-41048
        return emptySet()
    }

    @NoMutableState
    private inner class SymbolProvider : FirSymbolProvider(session) {
        override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> =
            providerHelper.getTopLevelCallableSymbols(packageFqName, name)

        @FirSymbolProviderInternals
        override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
            destination += getTopLevelCallableSymbols(packageFqName, name)
        }

        override fun getPackage(fqName: FqName): FqName? =
            providerHelper.getPackage(fqName)

        override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>? {
            return getFirClassifierByFqName(classId)?.symbol
        }
    }
}

internal val FirSession.firIdeProvider: FirIdeProvider by FirSession.sessionComponentAccessor()
