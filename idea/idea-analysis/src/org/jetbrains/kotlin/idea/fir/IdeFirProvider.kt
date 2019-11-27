/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

class IdeFirProvider(
    val project: Project,
    val scope: GlobalSearchScope,
    val builder: RawFirBuilder,
    val session: FirSession
) : FirProvider() {
    private val cacheProvider = FirProviderImpl(session)
    // TODO: invalidation?
    private val files = mutableMapOf<KtFile, FirFile>()

    override fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration<*>? {
        return cacheProvider.getFirClassifierByFqName(classId) ?: run {

            val classes = KotlinFullClassNameIndex.getInstance().get(classId.asSingleFqName().asString(), project, scope)
            val ktClass = classes.firstOrNull {
                classId.packageFqName == it.containingKtFile.packageFqName
            } ?: return null // TODO: what if two of them?
            val ktFile = ktClass.containingKtFile

            getOrBuildFile(ktFile)

            cacheProvider.getFirClassifierByFqName(classId)
        }
    }

    fun getOrBuildFile(ktFile: KtFile): FirFile {
        return files.getOrPut(ktFile) {
            val file = builder.buildFirFile(ktFile)
            cacheProvider.recordFile(file)
            file
        }
    }

    fun getFile(ktFile: KtFile): FirFile? = files[ktFile]

    override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>? {
        return getFirClassifierByFqName(classId)?.symbol
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        val packagePrefix = if (packageFqName.isRoot) "" else "$packageFqName."
        val topLevelFunctions = KotlinTopLevelFunctionFqnNameIndex.getInstance()["$packagePrefix$name", project, scope]
        val topLevelProperties = KotlinTopLevelPropertyFqnNameIndex.getInstance()["$packagePrefix$name", project, scope]
        topLevelFunctions.forEach { getOrBuildFile(it.containingKtFile) }
        topLevelProperties.forEach { getOrBuildFile(it.containingKtFile) }
        return cacheProvider.getTopLevelCallableSymbols(packageFqName, name)
    }

    override fun getFirClassifierContainerFile(fqName: ClassId): FirFile {
        getFirClassifierByFqName(fqName)
        return cacheProvider.getFirClassifierContainerFile(fqName)
    }

    override fun getFirClassifierContainerFileIfAny(fqName: ClassId): FirFile? {
        getFirClassifierByFqName(fqName)
        return cacheProvider.getFirClassifierContainerFileIfAny(fqName)
    }

    override fun getFirCallableContainerFile(symbol: FirCallableSymbol<*>): FirFile? {
        return cacheProvider.getFirCallableContainerFile(symbol)
    }

    override fun getFirFilesByPackage(fqName: FqName): List<FirFile> {
        val files = KotlinExactPackagesIndex.getInstance()[fqName.asString(), project, scope]
        files.forEach { getOrBuildFile(it) }
        return cacheProvider.getFirFilesByPackage(fqName)
    }

    override fun getClassDeclaredMemberScope(classId: ClassId): FirScope? {
        getFirClassifierByFqName(classId)
        return cacheProvider.getClassDeclaredMemberScope(classId)
    }

    override fun getClassUseSiteMemberScope(classId: ClassId, useSiteSession: FirSession, scopeSession: ScopeSession): FirScope? {
        getFirClassifierByFqName(classId)
        return cacheProvider.getClassUseSiteMemberScope(classId, useSiteSession, scopeSession)
    }
}