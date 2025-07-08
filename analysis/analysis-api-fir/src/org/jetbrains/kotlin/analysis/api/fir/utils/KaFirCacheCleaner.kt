/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.platform.KaCachedService
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionInvalidationService
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.LLStatisticsService
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.domains.LLAnalysisSessionStatistics
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFlightRecorder
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * A facility that cleans up all low-level resolution caches per request.
 */
@KaImplementationDetail
interface KaFirCacheCleaner {
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

    @KaImplementationDetail
    companion object {
        fun getInstance(project: Project): KaFirCacheCleaner {
            if (!Registry.`is`("kotlin.analysis.lowMemoryCacheCleanup", true)) {
                return KaFirNoOpCacheCleaner
            }

            return project.serviceOrNull<KaFirCacheCleaner>() ?: KaFirNoOpCacheCleaner
        }
    }
}

/**
 * An empty implementation of a cache cleaner – no additional cleanup is performed.
 * Can be used as a drop-in substitution for [KaFirStopWorldCacheCleaner] if forceful cache cleanup is disabled.
 */
private object KaFirNoOpCacheCleaner : KaFirCacheCleaner {
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
private class KaFirStopWorldCacheCleaner(private val project: Project) : KaFirCacheCleaner {
    private companion object {
        private val LOG = logger<KaFirStopWorldCacheCleaner>()

        private const val CACHE_CLEANER_LOCK_TIMEOUT_MS = 50L
    }

    private val lock = Any()

    @KaCachedService
    private val analysisSessionStatistics: LLAnalysisSessionStatistics? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLStatisticsService.getInstance(project)?.analysisSessions
    }

    @KaCachedService
    private val invalidationService: LLFirSessionInvalidationService by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLFirSessionInvalidationService.getInstance(project)
    }

    /**
     * The number of non-nested analyses running in parallel. In other words, the number of analyses in all threads.
     * Non-nested means that `analyze {}` blocks inside outer `analyze {}` blocks (both directly or indirectly) are not counted.
     *
     * [KaFirStopWorldCacheCleaner] counts ongoing sessions, and cleans caches up as soon as the last analysis block finishes execution.
     */
    @Volatile
    private var analyzerCount: Int = 0

    /**
     * The number of ongoing analyses in the current thread.
     * '0' means there is no ongoing analysis. '2' means there are two 'analyze {}' blocks in the stack.
     */
    private val analyzerDepth: ThreadLocal<Int> = ThreadLocal.withInitial { 0 }

    /**
     * A set of threads that currently have ongoing [analyze].
     */
    private val threadsWithAnalyze = ConcurrentHashMap.newKeySet<Thread>()

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

    override fun enterAnalysis() {
        // Avoid blocking nested analyses. This would break the logic, as the outer analysis will never complete
        // (because it will wait for the nested, this new one, to complete first). So we will never get to the point when all analyses
        // are done and clean the caches.
        if (hasOngoingAnalysis) {
            incAnalysisDepth()
            return
        }

        waitForCleanupIfNeeded()

        synchronized(lock) {
            // Register a top-level analysis block
            analyzerCount += 1
            threadsWithAnalyze.add(Thread.currentThread())
        }

        incAnalysisDepth()
    }

    override fun exitAnalysis() {
        decAnalysisDepth()

        // Ignore nested analyses (as in 'enterAnalysis')
        if (hasOngoingAnalysis) {
            return
        }

        synchronized(lock) {
            // Unregister a top-level analysis block
            analyzerCount -= 1
            threadsWithAnalyze.remove(Thread.currentThread())

            require(analyzerCount >= 0) { "Inconsistency in analyzer block counter" }

            if (cleanupLatch != null) {
                LOG.trace { "Analysis complete in ${Thread.currentThread()}, $analyzerCount left before the K2 cache cleanup" }
            }

            // Clean up the caches if there's a postponed cleanup, and we have no more analyses
            if (analyzerCount == 0) {
                val existingLatch = cleanupLatch
                if (existingLatch != null) {
                    try {
                        performCleanup()
                    } finally {
                        // Unpause all waiting analyses.
                        // Even if some new block comes before the 'cleanupLatch' is set to `null`, the old latch will be already open.
                        existingLatch.countDown()
                        cleanupLatch = null
                    }
                }
            }
        }
    }

    /**
     * If there's an ongoing cleanup request, wait until it's complete.
     *
     * Waiting for other threads might be tricky, because nothing prevents
     * from starting a new thread inside the [analyze] block where a new [analyze]
     * will be called.
     * Note: there are many ways to schedule some task and wait for it's completion (e.g. via threads pool),
     * so this is not only about pure threads creation.
     *
     * ### Example
     * ```kotlin
     * fun performSomeAction(declaration: KtDeclaration) { // parent thread
     *     analyze(declaration) { // 1 parent `analyze`
     *         val newThread = thread { // child thread
     *             // `scheduleCleanup` happens here
     *
     *             analyze(declaration) { // 2 child `analyze`
     *
     *             }
     *         }
     *
     *         while (newThread.isAlive) { // 3 parent waits for child
     *             ProgressManager.checkCanceled()
     *             newThread.join(50L)
     *         }
     *     }
     * }
     * ```
     *
     * Therefore, a naive approach with waiting for all other threads (except for [hasOngoingAnalysis] blocks)
     * might end up in deadlock ([KT-76577](https://youtrack.jetbrains.com/issue/KT-76577)).
     *
     * To mitigate the problem, a new approach with [threadsWithAnalyze] has been chosen to avoid deadlock.
     * But this is not a perfect solution, because it, at least theoretically, might lead to a livelock
     * in some corner cases.
     *
     * For instance, in the example above, the child thread will reach the loop inside [waitForCleanupIfNeeded] while
     * the parent thread will reach the loop (3) to wait for the child thread to finish.
     * Now imagine that it is so coincident that the parent thread is always in the [Thread.State.RUNNABLE] state
     * when the child makes another iteration to check for its state via [threadsWithAnalyze]. Here we have the livelock.
     * Hense, the more actions similar to `performSomeAction` we run in parallel, the higher the probability of ending up in the livelock.
     *
     * TODO: [KT-77093](https://youtrack.jetbrains.com/issue/KT-77093) potentially we might introduce some kind of versioning to our caches,
     * so we will be able to start new [analyze] actions without reusing the old caches, but already launched [analyze] actions will be able
     * to still access old caches, so their invariant will be preserved.
     *
     * @see cleanupLatch
     */
    private fun waitForCleanupIfNeeded() {
        val existingLatch = cleanupLatch ?: return

        val enterTime = System.currentTimeMillis()
        do {
            ProgressManager.checkCanceled()

            if (threadsWithAnalyze.none { it.state == Thread.State.RUNNABLE }) {
                val totalMs = System.currentTimeMillis() - enterTime
                LOG.trace { "A deadlock detected in K2 cache cleanup, ${Thread.currentThread()} is recovered after $totalMs ms" }
                break
            }
        } while (!existingLatch.await(CACHE_CLEANER_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS))
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
        synchronized(lock) {
            val existingLatch = cleanupLatch

            // Cleans the caches right away if there is no ongoing analysis or schedules a cleanup for later
            if (analyzerCount == 0) {
                // Here we perform cache invalidation without a read/write action wrapping guarantee.
                // We cannot start a new read/write action here as there might be already a pending write action waiting for 'this' monitor.
                // However, we are still sure no threads can get a session until the cleanup is complete.
                cleanupScheduleMs = System.currentTimeMillis()

                try {
                    performCleanup()
                } finally {
                    if (existingLatch != null) {
                        // Error recovery in case if things went really bad.
                        // Should never happen unless there is some flaw in the algorithm
                        existingLatch.countDown()
                        cleanupLatch = null
                        LOG.error("K2 cache cleanup was expected to happen right after the last analysis block completion")
                    }
                }
            } else if (existingLatch == null) {
                LOG.trace { "K2 cache cleanup scheduled from ${Thread.currentThread()}, $analyzerCount analyses left" }
                LLFlightRecorder.stopWorldSessionInvalidationScheduled()
                cleanupScheduleMs = System.currentTimeMillis()
                cleanupLatch = CountDownLatch(1)
            }
        }
    }

    /**
     * Cleans all K2 resolution caches.
     *
     * Must always run in `synchronized(lock)` to prevent concurrent cleanups.
     *
     * N.B.: May re-throw exceptions from IJ Platform [rethrowIntellijPlatformExceptionIfNeeded].
     */
    private fun performCleanup() {
        try {
            analysisSessionStatistics?.lowMemoryCacheCleanupInvocationCounter?.add(1)

            val cleanupMs = measureTimeMillis {
                invalidationService.invalidateAll(
                    includeLibraryModules = true,
                    diagnosticInformation = "low-memory cache cleanup",
                )
            }

            val totalMs = System.currentTimeMillis() - cleanupScheduleMs
            LOG.trace { "K2 cache cleanup complete from ${Thread.currentThread()} in $cleanupMs ms ($totalMs ms after the request)" }

            LLFlightRecorder.stopWorldSessionInvalidationComplete()
        } catch (e: Throwable) {
            rethrowIntellijPlatformExceptionIfNeeded(e)

            LOG.error("Could not clean up K2 caches", e)
        }
    }
}
