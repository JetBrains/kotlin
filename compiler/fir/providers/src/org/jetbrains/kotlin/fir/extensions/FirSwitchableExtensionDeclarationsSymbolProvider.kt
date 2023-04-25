/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/*
 * This provider is needed because we need to have ability to disable FirExtensionDeclarationsSymbolProvider during
 *   phase of annotations for plugins resolution. At this stage predicateBasedProvider is not indexed, so it will return
 *   empty results for all requests
 *
 * This is also legal, because plugins can not generate annotation classes which can influence other plugins or this plugin itself
 */
class FirSwitchableExtensionDeclarationsSymbolProvider private constructor(
    private val delegate: FirExtensionDeclarationsSymbolProvider
) : FirSymbolProvider(delegate.session) {
    companion object {
        fun createIfNeeded(session: FirSession): FirSwitchableExtensionDeclarationsSymbolProvider? =
            FirExtensionDeclarationsSymbolProvider.createIfNeeded(session)?.let { FirSwitchableExtensionDeclarationsSymbolProvider(it) }
    }

    private var disabled: Boolean = false

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        if (disabled) return null
        return delegate.getClassLikeSymbolByClassId(classId)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        if (disabled) return
        delegate.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        if (disabled) return
        delegate.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        if (disabled) return
        delegate.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
    }

    override fun getPackage(fqName: FqName): FqName? {
        if (disabled) return null
        return delegate.getPackage(fqName)
    }

    @FirSymbolProviderInternals
    fun disable() {
        disabled = true
    }

    @FirSymbolProviderInternals
    fun enable() {
        disabled = false
    }

    override fun computePackageSetWithTopLevelCallables(): Set<String>? =
        if (disabled) null else delegate.computePackageSetWithTopLevelCallables()

    override fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String>? =
        if (disabled) null else delegate.knownTopLevelClassifiersInPackage(packageFqName)

    override fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name>? =
        if (disabled) null else delegate.computeCallableNamesInPackage(packageFqName)

}

val FirSession.generatedDeclarationsSymbolProvider: FirSwitchableExtensionDeclarationsSymbolProvider? by FirSession.nullableSessionComponentAccessor()
