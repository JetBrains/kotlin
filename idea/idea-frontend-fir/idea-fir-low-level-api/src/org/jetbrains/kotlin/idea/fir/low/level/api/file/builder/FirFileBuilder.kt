/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.builder

import org.jetbrains.kotlin.fir.builder.RawFirBuilder
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
    private val firPhaseRunner: FirPhaseRunner
) {
    /**
     * Builds a [FirFile] by given [ktFile] and records it's parenting info if it not present in [cache]
     * [FirFile] building a happens at most once per each [KtFile]
     */
    fun buildRawFirFileWithCaching(
        ktFile: KtFile,
        cache: ModuleFileCache
    ): FirFile = cache.fileCached(ktFile) {
        RawFirBuilder(cache.session, scopeProvider, stubMode = false).buildFirFile(ktFile)
    }

    fun getFirFileResolvedToPhaseWithCaching(
        ktFile: KtFile,
        cache: ModuleFileCache,
        @Suppress("SameParameterValue") toPhase: FirResolvePhase
    ): FirFile {
        val firFile = buildRawFirFileWithCaching(ktFile, cache)
        if (toPhase > FirResolvePhase.RAW_FIR) {
            cache.firFileLockProvider.withLock(firFile) {
                //add lock for implit type resolve phase & super type
                runResolveWithoutLockNoPCECheck(firFile, fromPhase = firFile.resolvePhase, toPhase = toPhase)
            }
        }
        return firFile
    }

    /**
     * Runs [resolve] function (which is considered to do some resolve on [firFile]) under a lock for [firFile]
     */
    inline fun <R> runCustomResolveUnderLock(firFile: FirFile, cache: ModuleFileCache, resolve: () -> R): R =
        cache.firFileLockProvider.withLock(firFile) { resolve() }

    inline fun <R : Any> runCustomResolveWithPCECheck(firFile: FirFile, cache: ModuleFileCache, resolve: () -> R): R {
        val lock = cache.firFileLockProvider.getLockFor(firFile)
        return lock.lockWithPCECheck(LOCKING_INTERVAL_MS) { resolve() }
    }

    fun runResolveWithLock(firFile: FirFile, cache: ModuleFileCache, fromPhase: FirResolvePhase, toPhase: FirResolvePhase) {
        cache.firFileLockProvider.withLock(firFile) {
            runResolveWithoutLockNoPCECheck(firFile, fromPhase, toPhase)
        }
    }

    fun runResolveWithPCECheck(firFile: FirFile, cache: ModuleFileCache, fromPhase: FirResolvePhase, toPhase: FirResolvePhase) {
        val lock = cache.firFileLockProvider.getLockFor(firFile)
        lock.lockWithPCECheck(LOCKING_INTERVAL_MS) {
            val scopeSession = ScopeSession()
            var currentPhase = fromPhase
            while (currentPhase < toPhase) {
                checkCanceled()
                currentPhase = currentPhase.next
                firPhaseRunner.runPhase(firFile, currentPhase, scopeSession)
            }
        }
    }


    fun runResolveWithoutLockNoPCECheck(firFile: FirFile, fromPhase: FirResolvePhase, toPhase: FirResolvePhase) {
        assert(fromPhase <= toPhase) {
            "Trying to resolve file ${firFile.name} from $fromPhase to $toPhase"
        }
        val scopeSession = ScopeSession()
        var currentPhase = fromPhase
        while (currentPhase < toPhase) {
            currentPhase = currentPhase.next
            firPhaseRunner.runPhase(firFile, currentPhase, scopeSession)
        }
    }

    companion object {
        private const val LOCKING_INTERVAL_MS = 500L
    }
}


