/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.providers

import com.google.common.collect.Sets
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.DeclarationProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.KtPackageProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.util.FirElementFinder
import org.jetbrains.kotlin.idea.fir.low.level.api.util.executeOrReturnDefaultValueOnPCE
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import java.util.*

internal class FirProviderHelper(
    private val cache: ModuleFileCache,
    private val firFileBuilder: FirFileBuilder,
    private val declarationProvider: DeclarationProvider,
    private val packageProvider: KtPackageProvider,
) {
    fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration? {
        if (classId.isLocal) return null
        return executeOrReturnDefaultValueOnPCE(null) {
            cache.classifierByClassId.computeIfAbsent(classId) {
                val ktClass = when (val klass = declarationProvider.getClassesByClassId(classId).firstOrNull()) {
                    null -> declarationProvider.getTypeAliasesByClassId(classId).firstOrNull()
                    else -> if (klass.getClassId() == null) null else klass
                } ?: return@computeIfAbsent Optional.empty()
                val firFile = firFileBuilder.buildRawFirFileWithCaching(ktClass.containingKtFile, cache, preferLazyBodies = true)
                val classifier = FirElementFinder.findElementIn<FirClassLikeDeclaration>(firFile) { classifier ->
                    classifier.symbol.classId == classId
                }
                    ?: error("Classifier $classId was found in file ${ktClass.containingKtFile.virtualFilePath} but was not found in FirFile")
                Optional.of(classifier)
            }.getOrNull()
        }
    }


    fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        val callableId = CallableId(packageFqName, name)
        return executeOrReturnDefaultValueOnPCE(emptyList()) {
            cache.callableByCallableId.computeIfAbsent(callableId) {
                val files = Sets.newIdentityHashSet<KtFile>().apply {
                    declarationProvider.getTopLevelFunctions(callableId).mapTo(this) { it.containingKtFile }
                    declarationProvider.getTopLevelProperties(callableId).mapTo(this) { it.containingKtFile }
                }
                @OptIn(ExperimentalStdlibApi::class)
                buildList {
                    files.forEach { ktFile ->
                        val firFile = firFileBuilder.buildRawFirFileWithCaching(ktFile, cache, preferLazyBodies = true)
                        firFile.collectCallableDeclarationsTo(this, name)
                    }
                }
            }
        }
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
        fqName.takeIf(packageProvider::isPackageExists)
}
