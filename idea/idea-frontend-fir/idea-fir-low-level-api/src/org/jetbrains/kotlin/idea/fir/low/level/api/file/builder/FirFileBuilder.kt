/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.builder

import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.builder.RawFirBuilderMode
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.FirPhaseRunner
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.ThreadSafe
import org.jetbrains.kotlin.idea.fir.low.level.api.util.checkCanceled
import org.jetbrains.kotlin.idea.fir.low.level.api.util.lockWithPCECheck

/**
 * Responsible for building [FirFile] by [KtFile]
 * Stateless, all caches are stored in [ModuleFileCache] passed into corresponding functions
 */
@ThreadSafe
internal class FirFileBuilder(
    private val scopeProvider: FirScopeProvider,
    val firPhaseRunner: FirPhaseRunner
) {
    /**
     * Builds a [FirFile] by given [ktFile] and records it's parenting info if it not present in [cache]
     * [FirFile] building a happens at most once per each [KtFile]
     */
    fun buildRawFirFileWithCaching(
        ktFile: KtFile,
        cache: ModuleFileCache,
        lazyBodiesMode: Boolean
    ): FirFile = cache.fileCached(ktFile) {
        RawFirBuilder(cache.session, scopeProvider, RawFirBuilderMode.lazyBodies(lazyBodiesMode)).buildFirFile(ktFile)
    }

    fun getBuiltFirFileOrNull(ktFile: KtFile, cache: ModuleFileCache): FirFile? =
        cache.getCachedFirFile(ktFile)

    fun isFirFileBuilt(
        ktFile: KtFile,
        cache: ModuleFileCache
    ): Boolean = cache.getCachedFirFile(ktFile) != null

    fun getFirFileResolvedToPhaseWithCaching(
        ktFile: KtFile,
        cache: ModuleFileCache,
        @Suppress("SameParameterValue") toPhase: FirResolvePhase,
        scopeSession: ScopeSession,
        checkPCE: Boolean
    ): FirFile {
        val needResolve = toPhase > FirResolvePhase.RAW_FIR
        val firFile = buildRawFirFileWithCaching(ktFile, cache, lazyBodiesMode = !needResolve)
        if (needResolve) {
            cache.firFileLockProvider.withWriteLock(firFile) {
                if (firFile.resolvePhase >= toPhase) return@withWriteLock
                runResolveWithoutLock(
                    firFile,
                    fromPhase = firFile.resolvePhase,
                    toPhase = toPhase,
                    scopeSession = scopeSession,
                    checkPCE = checkPCE,
                )
            }
        }
        return firFile
    }

    /**
     * Runs [resolve] function (which is considered to do some resolve on [firFile]) under a lock for [firFile]
     */
    inline fun <R> runCustomResolveUnderLock(firFile: FirFile, cache: ModuleFileCache, resolve: () -> R): R =
        cache.firFileLockProvider.withWriteLock(firFile) { resolve() }

    inline fun <R : Any> runCustomResolveWithPCECheck(firFile: FirFile, cache: ModuleFileCache, resolve: () -> R): R =
        cache.firFileLockProvider.withWriteLockPCECheck(firFile, LOCKING_INTERVAL_MS, resolve)

    fun runResolveWithoutLock(
        firFile: FirFile,
        fromPhase: FirResolvePhase,
        toPhase: FirResolvePhase,
        scopeSession: ScopeSession,
        checkPCE: Boolean,
    ) {
        assert(fromPhase <= toPhase) {
            "Trying to resolve file ${firFile.name} from $fromPhase to $toPhase"
        }
        var currentPhase = fromPhase
        while (currentPhase < toPhase) {
            if (checkPCE) checkCanceled()
            currentPhase = currentPhase.next
            firPhaseRunner.runPhase(firFile, currentPhase, scopeSession)
        }
    }


    companion object {
        private const val LOCKING_INTERVAL_MS = 500L
    }
}


