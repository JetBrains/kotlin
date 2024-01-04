/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


class FirProviderWithGeneratedFiles(val session: FirSession, previousProviders: Map<FirModuleData, FirProviderWithGeneratedFiles>) : FirProvider() {
    private val generatedFilesProvider = FirProviderImpl(session, session.kotlinScopeProvider)

    private val providers: List<FirProvider> = buildList {
        add(session.firProvider)
        add(generatedFilesProvider)
        session.moduleData.dependsOnDependencies.mapNotNullTo(this) { previousProviders[it] }
    }

    override val symbolProvider: FirSymbolProvider
        get() = providers.first().symbolProvider

    override fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration? {
        return providers.firstNotNullOfOrNull { it.getFirClassifierByFqName(classId) }
    }

    override fun getFirClassifierContainerFile(fqName: ClassId): FirFile {
        return getFirClassifierContainerFileIfAny(fqName) ?: error("Couldn't find container for $fqName")
    }

    override fun getFirClassifierContainerFileIfAny(fqName: ClassId): FirFile? {
        return providers.firstNotNullOfOrNull { it.getFirClassifierContainerFileIfAny(fqName) }
    }

    override fun getFirCallableContainerFile(symbol: FirCallableSymbol<*>): FirFile? {
        return providers.firstNotNullOfOrNull { it.getFirCallableContainerFile(symbol) }
    }

    override fun getFirScriptContainerFile(symbol: FirScriptSymbol): FirFile? {
        return providers.firstNotNullOfOrNull { it.getFirScriptContainerFile(symbol) }
    }

    override fun getFirScriptByFilePath(path: String): FirScriptSymbol? {
        return providers.firstNotNullOfOrNull { it.getFirScriptByFilePath(path) }
    }

    override fun getFirFilesByPackage(fqName: FqName): List<FirFile> {
        return providers.flatMap { it.getFirFilesByPackage(fqName) }
    }

    override fun getClassNamesInPackage(fqName: FqName): Set<Name> {
        return providers.flatMapTo(mutableSetOf()) { it.getClassNamesInPackage(fqName) }
    }

    fun recordFile(file: FirFile) {
        generatedFilesProvider.recordFile(file)
    }
}
