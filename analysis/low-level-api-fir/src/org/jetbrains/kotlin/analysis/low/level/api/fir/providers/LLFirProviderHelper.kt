/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.google.common.collect.Sets
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirElementFinder
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

internal class LLFirProviderHelper(
    firSession: FirSession,
    private val firFileBuilder: LLFirFileBuilder,
    private val declarationProvider: KotlinDeclarationProvider,
    private val packageProvider: KotlinPackageProvider,
) {
    private val classifierByClassId = firSession.firCachesFactory.createCache<ClassId, FirClassLikeDeclaration?> { classId ->
        val ktClass = declarationProvider.getClassLikeDeclarationByClassId(classId)
            ?: return@createCache null
        if (ktClass.getClassId() == null) return@createCache null
        val firFile = firFileBuilder.buildRawFirFileWithCaching(ktClass.containingKtFile)
        FirElementFinder.findElementIn<FirClassLikeDeclaration>(
            firFile,
            canGoInside = { it is FirRegularClass },
            predicate = { it.symbol.classId == classId },
        )
            ?: error("Classifier $classId was found in file ${ktClass.containingKtFile.virtualFilePath} but was not found in FirFile")
    }


    private val callablesByCallableId = firSession.firCachesFactory.createCache<CallableId, List<FirCallableSymbol<*>>> { callableId ->
        val files = Sets.newIdentityHashSet<KtFile>().apply {
            declarationProvider.getTopLevelFunctions(callableId).mapTo(this) { it.containingKtFile }
            declarationProvider.getTopLevelProperties(callableId).mapTo(this) { it.containingKtFile }
        }
        buildList {
            files.forEach { ktFile ->
                val firFile = firFileBuilder.buildRawFirFileWithCaching(ktFile)
                firFile.collectCallableDeclarationsTo(this, callableId.callableName)
            }
        }
    }

    fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration? {
        if (classId.isLocal) return null
        return classifierByClassId.getValue(classId)
    }

    fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        val callableId = CallableId(packageFqName, name)
        return callablesByCallableId.getValue(callableId)
    }

    fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<FirNamedFunctionSymbol> {
        return getTopLevelCallableSymbols(packageFqName, name).filterIsInstance<FirNamedFunctionSymbol>()
    }

    fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<FirPropertySymbol> {
        return getTopLevelCallableSymbols(packageFqName, name).filterIsInstance<FirPropertySymbol>()
    }

    private fun FirFile.collectCallableDeclarationsTo(list: MutableList<FirCallableSymbol<*>>, name: Name) {
        declarations.mapNotNullTo(list) { declaration ->
            if (declaration is FirCallableDeclaration && declaration.symbol.callableId.callableName == name) {
                declaration.symbol
            } else null
        }
    }

    fun getPackage(fqName: FqName): FqName? =
        fqName.takeIf(packageProvider::doKotlinPackageExists)
}
