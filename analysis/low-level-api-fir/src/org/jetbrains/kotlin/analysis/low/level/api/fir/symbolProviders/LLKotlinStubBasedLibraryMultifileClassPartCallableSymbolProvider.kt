/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.JvmAndBuiltinsDeserializedContainerSourceProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Issue: [KT-68484](https://youtrack.jetbrains.com/issue/KT-68484).
 *
 * This class provides fallback symbols for top-level callables from synthetic multifile class part
 * ([MULTIFILE_CLASS_PART][org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader.Kind.MULTIFILE_CLASS_PART]).
 *
 * @see org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsClassFinder.isKotlinInternalCompiledFile
 * @see addCallableIfNeeded
 **/
internal class LLKotlinStubBasedLibraryMultifileClassPartCallableSymbolProvider(val session: FirSession) {
    private val fallbackFunctionCache = session.firCachesFactory.createCache(::loadFunction)
    private val fallbackPropertyCache = session.firCachesFactory.createCache(::loadProperty)

    /**
     * This fallback is required for multifile part classes which are not present in indices
     * but might be requested as in some cases we still build stubs for them.
     */
    fun addCallableIfNeeded(
        callableCandidates: MutableList<FirCallableSymbol<*>>,
        packageFqName: FqName,
        shortName: Name,
        callableDeclaration: KtCallableDeclaration,
    ) {
        val fileName = callableDeclaration.containingKtFile.virtualFile?.nameWithoutExtension ?: return
        if (!fileName.endsWith("Kt") || JvmStandardClassIds.MULTIFILE_PART_NAME_DELIMITER !in fileName) {
            return
        }

        val callableId = CallableId(packageFqName, shortName)
        val symbol = when (callableDeclaration) {
            is KtNamedFunction -> fallbackFunctionCache.getValue(callableDeclaration, callableId)
            is KtProperty -> fallbackPropertyCache.getValue(callableDeclaration, callableId)
            else -> null
        }

        symbol?.let(callableCandidates::add)
    }

    private fun loadFunction(function: KtNamedFunction, callableId: CallableId): FirNamedFunctionSymbol? {
        return LLKotlinStubBasedLibrarySymbolProvider.loadFunction(
            function = function,
            callableId = callableId,
            functionOrigin = FirDeclarationOrigin.Library,
            deserializedContainerSourceProvider = JvmAndBuiltinsDeserializedContainerSourceProvider,
            session = session,
        )
    }

    private fun loadProperty(property: KtProperty, callableId: CallableId): FirPropertySymbol? {
        return LLKotlinStubBasedLibrarySymbolProvider.loadProperty(
            property = property,
            callableId = callableId,
            propertyOrigin = FirDeclarationOrigin.Library,
            deserializedContainerSourceProvider = JvmAndBuiltinsDeserializedContainerSourceProvider,
            session = session,
        )
    }
}