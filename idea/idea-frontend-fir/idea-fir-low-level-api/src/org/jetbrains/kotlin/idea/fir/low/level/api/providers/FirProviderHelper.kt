/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.providers

import com.google.common.collect.Sets
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.util.FirElementFinder
import org.jetbrains.kotlin.idea.fir.low.level.api.IndexHelper
import org.jetbrains.kotlin.idea.fir.low.level.api.PackageExistenceChecker
import org.jetbrains.kotlin.idea.fir.low.level.api.util.executeOrReturnDefaultValueOnPCE
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import java.util.*

internal class FirProviderHelper(
    private val cache: ModuleFileCache,
    private val firFileBuilder: FirFileBuilder,
    private val indexHelper: IndexHelper,
    private val packageExistenceChecker: PackageExistenceChecker,
) {
    fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration<*>? {
        return executeOrReturnDefaultValueOnPCE(null) {
            cache.classifierByClassId.computeIfAbsent(classId) {
                val ktClass = indexHelper.classFromIndexByClassId(classId)
                    ?: indexHelper.typeAliasFromIndexByClassId(classId)
                    ?: return@computeIfAbsent Optional.empty()
                if (ktClass is KtEnumEntry) return@computeIfAbsent Optional.empty()
                val firFile = firFileBuilder.buildRawFirFileWithCaching(ktClass.containingKtFile, cache)
                val classifier = FirElementFinder.findElementIn<FirClassLikeDeclaration<*>>(firFile) { classifier ->
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
                    indexHelper.getTopLevelFunctions(callableId).mapTo(this) { it.containingKtFile }
                    indexHelper.getTopLevelProperties(callableId).mapTo(this) { it.containingKtFile }
                }
                @OptIn(ExperimentalStdlibApi::class)
                buildList {
                    files.forEach { ktFile ->
                        val firFile = firFileBuilder.buildRawFirFileWithCaching(ktFile, cache)
                        firFile.collectCallableDeclarationsTo(this, name)
                    }
                }
            }
        }
    }

    private fun FirFile.collectCallableDeclarationsTo(list: MutableList<FirCallableSymbol<*>>, name: Name) {
        declarations.mapNotNullTo(list) { declaration ->
            if (declaration is FirCallableDeclaration<*> && declaration.symbol.callableId.callableName == name) {
                declaration.symbol
            } else null
        }
    }

    fun getNestedClassifierScope(classId: ClassId): FirScope? =
        (getFirClassifierByFqName(classId) as? FirRegularClass)?.let {
            nestedClassifierScope(it)
        }

    fun getPackage(fqName: FqName): FqName? =
        fqName.takeIf(packageExistenceChecker::isPackageExists)
}