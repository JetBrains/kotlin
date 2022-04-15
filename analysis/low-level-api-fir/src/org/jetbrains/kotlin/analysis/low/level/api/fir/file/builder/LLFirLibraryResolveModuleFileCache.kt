/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerDataContextAllElementsCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FirElementsRecorder
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.KtToFirMapping
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirLibraryOrLibrarySourceResolvableModuleSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtFile
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal class LLFirLibraryResolveModuleFileCache: ModuleFileCache() {

    private val kfFileToFirCache = ConcurrentHashMap<KtFile, ResolvedFile>()

    override val classifierByClassId: ConcurrentHashMap<ClassId, Optional<FirClassLikeDeclaration>> get() = error("Should not be called")
    override val callableByCallableId: ConcurrentHashMap<CallableId, List<FirCallableSymbol<*>>> get() = error("Should not be called")

    override fun fileCached(file: KtFile, createValue: () -> FirFile): FirFile {
        return getResolvedFile(file) { createValue() }.firFile
    }

    override fun getContainerFirFile(declaration: FirDeclaration): FirFile? {
        val ktFile = declaration.psi?.containingFile as? KtFile ?: return null
        return getCachedFirFile(ktFile)
    }

    override fun getCachedFirFile(ktFile: KtFile): FirFile? {
        return kfFileToFirCache[ktFile]?.firFile
    }

    override val firFileLockProvider: LockProvider<FirFile> = LockProvider()

    fun getResolvedFile(
        ktFile: KtFile,
        createKtFile: (KtFile) -> FirFile
    ): ResolvedFile = kfFileToFirCache.computeIfAbsent(ktFile) {
        val collector = FirTowerDataContextAllElementsCollector()

        val scopeSession = moduleComponents.scopeSessionProvider.getScopeSession()

        val firFile = createKtFile(ktFile)

        moduleComponents.lazyFirDeclarationsResolver.lazyResolveFileDeclaration(
            firFile,
            FirResolvePhase.BODY_RESOLVE,
            scopeSession,
            collector,
            checkPCE = true
        )

        ResolvedFile(
            firFile,
            KtToFirMapping(firFile, FirElementsRecorder()),
            collector
        )
    }

    class ResolvedFile(
        val firFile: FirFile,
        val mapping: KtToFirMapping,
        val collector: FirTowerDataContextAllElementsCollector,
    )
}