/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.KotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

class FirIdeProvider(
    val project: Project,
    val scope: GlobalSearchScope,
    val session: FirSession,
    kotlinScopeProvider: KotlinScopeProvider
) : FirProvider() {
    private val cacheProvider = FirProviderImpl(session, kotlinScopeProvider)

    data class FirFileWithStamp(val file: FirFile, val stamp: Long)

    private val files = mutableMapOf<KtFile, FirFileWithStamp>()

    override val isPhasedFirAllowed: Boolean
        get() = true

    override fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration<*>? {
        return cacheProvider.getFirClassifierByFqName(classId) ?: run {
            try {
                val classes = KotlinFullClassNameIndex.getInstance().get(classId.asSingleFqName().asString(), project, scope)
                val ktClass = classes.firstOrNull {
                    classId.packageFqName == it.containingKtFile.packageFqName
                } ?: return null // TODO: what if two of them?

                val ktFile = ktClass.containingKtFile
                getOrBuildFile(ktFile)

                cacheProvider.getFirClassifierByFqName(classId)
            } catch (e: ProcessCanceledException) {
                return null
            }
        }
    }

    fun getOrBuildFile(ktFile: KtFile): FirFile {
        val modificationStamp = ktFile.modificationStamp
        files[ktFile]?.let { (firFile, stamp) ->
            if (stamp == modificationStamp) {
                return firFile
            }
        }
        return synchronized(ktFile) {
            var fileWithStamp = files[ktFile]
            if (fileWithStamp != null && fileWithStamp.stamp == modificationStamp) {
                fileWithStamp.file
            } else {
                val file = RawFirBuilder(session, cacheProvider.kotlinScopeProvider, stubMode = false).buildFirFile(ktFile)
                cacheProvider.recordFile(file)
                fileWithStamp = FirFileWithStamp(file, modificationStamp)
                files[ktFile] = fileWithStamp
                file
            }
        }
    }

    fun getFile(ktFile: KtFile): FirFile? {
        val (firFile, stamp) = files[ktFile] ?: return null
        if (stamp == ktFile.modificationStamp) return firFile
        return null
    }

    override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>? {
        return getFirClassifierByFqName(classId)?.symbol
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        val packagePrefix = if (packageFqName.isRoot) "" else "$packageFqName."
        return try {
            val topLevelFunctions = KotlinTopLevelFunctionFqnNameIndex.getInstance()["$packagePrefix$name", project, scope]
            val topLevelProperties = KotlinTopLevelPropertyFqnNameIndex.getInstance()["$packagePrefix$name", project, scope]
            topLevelFunctions.forEach { getOrBuildFile(it.containingKtFile) }
            topLevelProperties.forEach { getOrBuildFile(it.containingKtFile) }
            cacheProvider.getTopLevelCallableSymbols(packageFqName, name)
        } catch (e: ProcessCanceledException) {
            emptyList()
        }
    }

    override fun getFirClassifierContainerFile(fqName: ClassId): FirFile {
        getFirClassifierByFqName(fqName) // Necessary to ensure cacheProvider contains this classifier
        return cacheProvider.getFirClassifierContainerFile(fqName)
    }

    override fun getFirClassifierContainerFileIfAny(fqName: ClassId): FirFile? {
        getFirClassifierByFqName(fqName) // Necessary to ensure cacheProvider contains this classifier
        return cacheProvider.getFirClassifierContainerFileIfAny(fqName)
    }

    override fun getFirClassifierContainerFile(symbol: FirClassLikeSymbol<*>): FirFile {
        return getFirClassifierContainerFileIfAny(symbol)
            ?: error("Couldn't find container for ${symbol.classId}")
    }

    override fun getFirClassifierContainerFileIfAny(symbol: FirClassLikeSymbol<*>): FirFile? {
        val psi = symbol.fir.source?.psi
        if (psi is KtElement) {
            return try {
                val ktFile = psi.containingKtFile
                getOrBuildFile(ktFile)
            } catch (e: ProcessCanceledException) {
                null
            }
        }
        return getFirClassifierContainerFileIfAny(symbol.classId)
    }

    override fun getFirCallableContainerFile(symbol: FirCallableSymbol<*>): FirFile? {
        return cacheProvider.getFirCallableContainerFile(symbol)
    }

    override fun getFirFilesByPackage(fqName: FqName): List<FirFile> {
        return try {
            val files = KotlinExactPackagesIndex.getInstance()[fqName.asString(), project, scope]
            files.forEach { getOrBuildFile(it) }
            cacheProvider.getFirFilesByPackage(fqName)
        } catch (e: ProcessCanceledException) {
            emptyList()
        }
    }

    override fun getNestedClassifierScope(classId: ClassId): FirScope? {
        getFirClassifierByFqName(classId)
        return cacheProvider.getNestedClassifierScope(classId)
    }
}