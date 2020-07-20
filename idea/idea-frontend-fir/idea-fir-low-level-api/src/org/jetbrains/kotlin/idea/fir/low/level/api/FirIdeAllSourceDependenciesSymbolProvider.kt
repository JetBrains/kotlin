/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.module.impl.scopes.ModuleScopeProviderImpl
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement

internal class FirIdeAllSourceDependenciesSymbolProvider(
    project: Project,
    moduleInfo: ModuleSourceInfo,
    private val sessionProvider: FirIdeSessionProvider,
    private val firBuilder: FirBuilder,
) : FirSymbolProvider() {
    private val scope = ModuleScopeProviderImpl(moduleInfo.module).moduleWithDependenciesScope
    private val indexHelper = IndexHelper(project)
    private val packageExistenceChecker = PackageExistenceCheckerForMultipleModules(project, moduleInfo.dependencies())

    private val cache = PsiToFirCacheImpl()

    override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>? {
        return executeOrReturnDefaultValueOnPCE(null) {
            val ktClass = indexHelper.firstMatchingOrNull(
                KotlinFullClassNameIndex.KEY,
                classId.asSingleFqName().asString(),
                scope
            ) { candidate -> candidate.containingKtFile.packageFqName == classId.packageFqName } ?: return null
            (ktClass.toFir() as FirClassLikeDeclaration<*>).symbol
        }
    }


    @OptIn(ExperimentalStdlibApi::class)
    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        val callableId = CallableId(packageFqName, name)
        return executeOrReturnDefaultValueOnPCE(emptyList()) {
            buildList {
                indexHelper.getTopLevelFunctions(callableId, scope).mapTo(this) { (it.toFir() as FirCallableDeclaration<*>).symbol }
                indexHelper.getTopLevelProperties(callableId, scope).mapTo(this) { (it.toFir() as FirCallableDeclaration<*>).symbol }
            }
        }
    }

    private fun KtElement.toFir(): FirElement =
        getOrBuildFir(cache, firBuilder, sessionProvider, FirResolvePhase.RAW_FIR)

    override fun getNestedClassifierScope(classId: ClassId): FirScope? {
        val classifier = getClassLikeSymbolByFqName(classId) ?: return null
        return classifier.fir.session.firIdeProvider.getNestedClassifierScope(classId)
    }

    override fun getPackage(fqName: FqName): FqName? =
        fqName.takeIf(packageExistenceChecker::isPackageExists)
}