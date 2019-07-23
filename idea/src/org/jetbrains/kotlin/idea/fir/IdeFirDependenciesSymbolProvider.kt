/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.AbstractFirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelTypeAliasFqNameIndex
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeAlias

class IdeFirDependenciesSymbolProvider(
    val moduleInfo: ModuleSourceInfo,
    val project: Project,
    private val sessionProvider: FirProjectSessionProvider
) : AbstractFirSymbolProvider() {

    // TODO: Our special scope here?
    private val depScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(moduleInfo.module)

    private val javaSymbolProvider by lazy { JavaSymbolProvider(sessionProvider.getSession(moduleInfo)!!, project, depScope) }


    private fun buildKotlinClassOnRequest(file: KtFile, classId: ClassId, session: FirSession): FirClassLikeSymbol<*>? {
        val impl = FirProvider.getInstance(session) as FirProviderImpl
        val classifier = impl.getClassLikeSymbolByFqName(classId)
        if (classifier != null) {
            return classifier
        }

        val builder = RawFirBuilder(session, stubMode = true)
        impl.recordFile(builder.buildFirFile(file))
        return impl.getClassLikeSymbolByFqName(classId)
    }

    private fun selectNearest(classesPsi: Collection<KtDeclaration>, typeAliasesPsi: Collection<KtTypeAlias>): KtDeclaration? {
        return when {
            typeAliasesPsi.isEmpty() -> classesPsi.firstOrNull()
            classesPsi.isEmpty() -> typeAliasesPsi.firstOrNull()
            else -> (classesPsi.asSequence() + typeAliasesPsi.asSequence()).minWith(Comparator { o1, o2 ->
                depScope.compare(o1.containingFile.virtualFile, o2.containingFile.virtualFile)
            })
        }
    }

    override fun getClassUseSiteMemberScope(classId: ClassId, useSiteSession: FirSession, scopeSession: ScopeSession): FirScope? {
        TODO("not implemented")
    }

    private fun tryKotlin(classId: ClassId): FirClassLikeSymbol<*>? {
        return classCache.lookupCacheOrCalculate(classId) {
            val index = KotlinFullClassNameIndex.getInstance()

            val fqNameString = classId.packageFqName.asString() + "." + classId.relativeClassName.asString()
            val classesPsi = index[fqNameString, project, depScope]
            val typeAliasesPsi = KotlinTopLevelTypeAliasFqNameIndex.getInstance()[fqNameString, project, depScope]

            val psi = selectNearest(classesPsi, typeAliasesPsi) ?: return@lookupCacheOrCalculate null

            val module = psi.getModuleInfo()
            if (psi.containingKtFile.isCompiled) {
                // TODO: WTF? Resolving libraries in current session
                val session = sessionProvider.getSession(moduleInfo) ?: return@lookupCacheOrCalculate null
                return@lookupCacheOrCalculate buildKotlinClassOnRequest(psi.containingKtFile, classId, session)
            }

            val session = sessionProvider.getSession(module) ?: return@lookupCacheOrCalculate null
            session.service<FirProvider>().getClassLikeSymbolByFqName(classId)
        }
    }

    private fun tryJava(classId: ClassId): FirClassLikeSymbol<*>? {
        return javaSymbolProvider.getClassLikeSymbolByFqName(classId)
    }

    override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>? {
        return tryKotlin(classId) ?: tryJava(classId)
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        // TODO
        return emptyList()
    }

    override fun getClassDeclaredMemberScope(classId: ClassId): FirScope? = null

    override fun getPackage(fqName: FqName): FqName? {

        if (PackageIndexUtil.packageExists(fqName, depScope, project)) {
            return fqName
        }
        return javaSymbolProvider.getPackage(fqName)
    }
}
