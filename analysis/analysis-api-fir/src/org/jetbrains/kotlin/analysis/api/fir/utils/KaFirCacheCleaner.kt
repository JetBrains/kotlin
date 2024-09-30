/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinReadActionConfinementLifetimeToken
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionInvalidationService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * A facility that cleans up all low-level resolution caches per request.
 */
internal interface KaFirCacheCleaner {
    /**
     * This method must be called before the [KaSession] is obtained (to be used later in an [analyze] block).
     * If the method is called, [exitAnalysis] must also be called from the same thread right after the block finishes executing, or
     * if analysis fails with some error (even if the block didn't start executing yet).
     */
    fun enterAnalysis()

    /**
     * This method must be called right after an [analyze] block finishes executing.
     * It is a counterpart for [enterAnalysis].
     */
    fun exitAnalysis()

    /**
     * Schedule analysis cache cleanup.
     * The actual cleanup may happen immediately (if it's possible to do so), or some time later.
     *
     * Consequent calls to [scheduleCleanup] are permitted (and ignored if a cleanup is already scheduled).
     */
    fun scheduleCleanup()
}

/**
 * An empty implementation of a cache cleaner – no additional cleanup is performed.
 * Can be used as a drop-in substitution for [KaFirStopWorldCacheCleaner] if forceful cache cleanup is disabled.
 */
internal object KaFirNoOpCacheCleaner : KaFirCacheCleaner {
    override fun enterAnalysis() {}
    override fun exitAnalysis() {}
    override fun scheduleCleanup() {}
}

/**
 * A stop-the-world implementation of a cache cleaner.
 *
 * It's impossible to clean up caches at a random point, as ongoing analyses will fail because their use-site sessions will be invalidated.
 * So [KaFirStopWorldCacheCleaner] registers cleanup events and waits until all analysis blocks finish executing.
 * Once a cleanup is requested, the class also prevents all new analysis blocks from running until it's complete (see the [cleanupLatch]).
 * If there is no ongoing analysis, though, caches can be cleaned up immediately.
 */
internal class KaFirStopWorldCacheCleaner(private val project: Project) : KaFirCacheCleaner {
    private companion object {
        private val LOG = Logger.getInstance(KaFirStopWorldCacheCleaner::class.java)

        private const val CACHE_CLEANER_LOCK_TIMEOUT_MS = 50L
    }

    /**
     * The number of non-nested analyses running in parallel. In other words, the number of analyses in all threads.
     * Non-nested means that `analyze {}` blocks inside outer `analyze {}` blocks (both directly or indirectly) are not counted.
     *
     * [KaFirStopWorldCacheCleaner] counts ongoing sessions, and cleans caches up as soon as the last analysis block finishes execution.
     */
    @Volatile
    private var analyzerCount = 0

    /**
     * The number of ongoing analyses in the current thread.
     * '0' means there is no ongoing analysis. '2' means there are two 'analyze {}' blocks in the stack.
     */
    private val analyzerDepth = ThreadLocal.withInitial<Int> { 0 }

    /**
     * A latch preventing newly created analyses from running until the cache cleanup is complete.
     * [cleanupLatch] is `null` when there is no scheduled (postponed) cleanup.
     */
    @Volatile
    private var cleanupLatch: CountDownLatch? = null

    /**
     * A timestamp when the currently postponed cleanup is scheduled, or an arbitrary value if no cleanup is scheduled.
     */
    @Volatile
    private var cleanupScheduleMs: Long = 0

    /**
     * `true` if there is an ongoing analysis in the current thread.
     * It means that a newly registered analysis block will be a nested one.
     */
    private val hasOngoingAnalysis: Boolean
        get() = analyzerDepth.get() > 0

    /**
     * `true` if the analysis block is running under a read/write action.
     *
     * `analyze {}` requires at least a read action to proceed, so an exception will likely be thrown from
     * [KotlinReadActionConfinementLifetimeToken]. However, [enterAnalysis] and [exitAnalysis] will still be called for such cases.
     * This flag deactivates all calculation logic for incorrect `analyze {}` calls.
     */
    private val isAnalysisAllowed: Boolean
        get() = ApplicationManager.getApplication().isReadAccessAllowed

    override fun enterAnalysis() {
        // Avoid blocking nested analyses. This would break the logic, as the outer analysis will never complete
        // (because it will wait for the nested, this new one, to complete first). So we will never get to the point when all analyses
        // are done and clean the caches.
        if (hasOngoingAnalysis) {
            incAnalysisDepth()
            return
        }

        val existingLatch = cleanupLatch
        if (existingLatch != null) {
            // If there's an ongoing cleanup request, wait until it's complete
            while (!existingLatch.await(CACHE_CLEANER_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                ProgressManager.checkCanceled()
            }
        }

        // Skip state-changing logic for incorrect analysis blocks.
        if (!isAnalysisAllowed) {
            incAnalysisDepth()
            return
        }

        synchronized(this) {
            // Register a top-level analysis block
            analyzerCount += 1
        }

        incAnalysisDepth()
    }

    override fun exitAnalysis() {
        decAnalysisDepth()

        // Ignore nested and invalid analyses (as in 'enterAnalysis')
        if (!isAnalysisAllowed || hasOngoingAnalysis) {
            return
        }

        synchronized(this) {
            // Unregister a top-level analysis block
            analyzerCount -= 1

            require(analyzerCount >= 0) { "Inconsistency in analyzer block counter" }

            if (cleanupLatch != null) {
                LOG.debug { "Analysis complete in ${Thread.currentThread()}, $analyzerCount left before the K2 cache cleanup" }
            }

            // Clean up the caches if there's a postponed cleanup, and we have no more analyses
            if (analyzerCount == 0) {
                val existingLatch = cleanupLatch
                if (existingLatch != null) {
                    performCleanup()

                    // Unpause all waiting analyses.
                    // Even if some new block comes before the 'cleanupLatch' is set to `null`, the old latch will be already open.
                    existingLatch.countDown()
                    cleanupLatch = null
                }
            }
        }
    }

    private fun incAnalysisDepth() {
        analyzerDepth.set(analyzerDepth.get() + 1)
    }

    private fun decAnalysisDepth() {
        val oldValue = analyzerDepth.get()
        assert(oldValue > 0) { "Inconsistency in analysis depth counter" }
        analyzerDepth.set(oldValue - 1)
    }

    override fun scheduleCleanup() {
        synchronized(this) {
            val existingLatch = cleanupLatch

            // Cleans the caches right away if there is no ongoing analysis or schedules a cleanup for later
            if (analyzerCount == 0) {
                // Here we perform cache invalidation without a read/write action wrapping guarantee.
                // We cannot start a new read/write action here as there might be already a pending write action waiting for 'this' monitor.
                // However, we are still sure no threads can get a session until the cleanup is complete.
                cleanupScheduleMs = System.currentTimeMillis()
                performCleanup()

                if (existingLatch != null) {
                    // Error recovery in case if things went really bad.
                    // Should never happen unless there is some flaw in the algorithm
                    existingLatch.countDown()
                    cleanupLatch = null
                    LOG.error("K2 cache cleanup was expected to happen right after the last analysis block completion")
                }
            } else if (existingLatch == null) {
                LOG.debug { "K2 cache cleanup scheduled from ${Thread.currentThread()}, $analyzerCount analyses left" }
                cleanupScheduleMs = System.currentTimeMillis()
                cleanupLatch = CountDownLatch(1)
            }
        }
    }

    /**
     * Cleans all K2 resolution caches.
     *
     * Must always run in `synchronized(this)` to prevent concurrent cleanups.
     */
    @OptIn(LLFirInternals::class)
    private fun performCleanup() {
        try {
            val cleanupMs = measureTimeMillis {
                val invalidationService = LLFirSessionInvalidationService.getInstance(project)
                invalidationService.invalidateAll(includeLibraryModules = true)
            }
            val totalMs = System.currentTimeMillis() - cleanupScheduleMs
            LOG.debug { "K2 cache cleanup complete from ${Thread.currentThread()} in $cleanupMs ms ($totalMs ms after the request)" }
        } catch (e: Throwable) {
            LOG.error("Could not clean up K2 caches", e)
        }
    }
}
