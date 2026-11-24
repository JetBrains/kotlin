/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.listeners

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.fir.utils.KaFirCacheCleaner
import org.jetbrains.kotlin.analysis.api.fir.utils.KaFirStopWorldCacheCleaner
import org.jetbrains.kotlin.analysis.api.platform.KotlinLowMemoryCacheCleanupListener
import org.jetbrains.kotlin.analysis.api.platform.KotlinLowMemoryCacheCleanupListener.WaitOutcome
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.standalone.fir.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.services.TestServices
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Checks the events which [KaFirStopWorldCacheCleaner] publishes to [KotlinLowMemoryCacheCleanupListener].
 */
@Execution(ExecutionMode.SAME_THREAD)
class KotlinLowMemoryCacheCleanupListenerTest : AbstractAnalysisApiExecutionTest("testData/listeners/lowMemoryCacheCleanup") {
    override val configurator = LLSourceLikeTestConfigurator()

    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = super.additionalServiceRegistrars + StopWorldCacheCleanerTestServiceRegistrar

    @Test
    @TestMetadata("cleanup")
    fun testImmediateCleanup(mainFile: KtFile) {
        val events = recordEvents(mainFile) {
            KaFirCacheCleaner.getInstance(mainFile.project).scheduleCleanup()
        }

        assertEquals(listOf("afterCleanup(isDeferred=false)"), events)
    }

    @Test
    @TestMetadata("cleanup")
    fun testDeferredCleanup(mainFile: KtFile) {
        val events = recordEvents(mainFile) { events ->
            analyze(mainFile.simpleClass) {
                KaFirCacheCleaner.getInstance(mainFile.project).scheduleCleanup()
                events.add("end of 'analyze' block")
            }
        }

        assertEquals(listOf("end of 'analyze' block", "afterCleanup(isDeferred=true)"), events)
    }

    /**
     * The analysis on the test thread keeps running until the other thread waits for the deferred cleanup. Because an analysis thread is
     * running, the cleaner doesn't suspect a deadlock, and the other thread waits until the cleanup is complete.
     */
    @Test
    @TestMetadata("cleanup")
    fun testWaitForDeferredCleanup(mainFile: KtFile) {
        val events = recordEvents(mainFile) {
            analyze(mainFile.simpleClass) {
                KaFirCacheCleaner.getInstance(mainFile.project).scheduleCleanup()

                val waitingThread = startAnalysisThread(mainFile)

                // Keep the current thread `RUNNABLE` (no sleeping) until the other thread waits for the cleanup.
                val deadline = System.currentTimeMillis() + TIMEOUT_MS
                while (waitingThread.thread.state != Thread.State.TIMED_WAITING) {
                    if (System.currentTimeMillis() > deadline) fail("The analysis thread did not start waiting for the cleanup.")
                }

                // Leaving this `analyze` block performs the cleanup, so the other thread can only finish afterward.
                waitingThread
            }.joinAndCheck()
        }

        assertEquals(
            setOf("afterCleanup(isDeferred=true)", "afterWaitingForCleanup(outcome=${WaitOutcome.CLEANUP_COMPLETED})"),
            events.toSet(),
        )
        assertEquals(2, events.size)
    }

    /**
     * The analysis on the test thread waits for an analysis on another thread, which in turn would wait for the deferred cleanup (see
     * KT-76577). The cleaner suspects the deadlock because no analysis thread is running, and the other thread stops waiting.
     */
    @Test
    @TestMetadata("cleanup")
    fun testSuspectedDeadlock(mainFile: KtFile) {
        val events = recordEvents(mainFile) { events ->
            analyze(mainFile.simpleClass) {
                KaFirCacheCleaner.getInstance(mainFile.project).scheduleCleanup()

                startAnalysisThread(mainFile).joinAndCheck()
                events.add("end of 'analyze' block")
            }
        }

        assertEquals(
            listOf(
                "afterWaitingForCleanup(outcome=${WaitOutcome.DEADLOCK_SUSPECTED})",
                "end of 'analyze' block",
                "afterCleanup(isDeferred=true)",
            ),
            events,
        )
    }

    private val KtFile.simpleClass: KtClass
        get() = declarations.single { it is KtClass && it.name == "Simple" } as KtClass

    private fun recordEvents(mainFile: KtFile, action: (MutableCollection<String>) -> Unit): List<String> {
        val events = ConcurrentLinkedQueue<String>()
        val connection = mainFile.project.analysisMessageBus.connect()
        try {
            connection.subscribe(KotlinLowMemoryCacheCleanupListener.TOPIC, RecordingListener(events))
            action(events)
        } finally {
            connection.disconnect()
        }
        return events.toList()
    }

    private class RecordingListener(private val events: MutableCollection<String>) : KotlinLowMemoryCacheCleanupListener {
        override fun afterCleanup(invalidationDurationMs: Long, requestToCompletionDurationMs: Long, isDeferred: Boolean) {
            assertTrue(invalidationDurationMs >= 0)
            assertTrue(requestToCompletionDurationMs >= invalidationDurationMs)
            events.add("afterCleanup(isDeferred=$isDeferred)")
        }

        override fun afterWaitingForCleanup(waitDurationMs: Long, outcome: WaitOutcome) {
            assertTrue(waitDurationMs >= 0)
            events.add("afterWaitingForCleanup(outcome=$outcome)")
        }
    }

    private class AnalysisThread(val thread: Thread, private val failure: AtomicReference<Throwable?>) {
        fun joinAndCheck() {
            thread.join(TIMEOUT_MS)
            if (thread.isAlive) fail("The analysis thread did not finish in time.")
            failure.get()?.let { throw it }
        }
    }

    private fun startAnalysisThread(mainFile: KtFile): AnalysisThread {
        val failure = AtomicReference<Throwable?>(null)
        val thread = thread(name = "KotlinLowMemoryCacheCleanupListenerTest analysis") {
            try {
                analyze(mainFile.simpleClass) {}
            } catch (e: Throwable) {
                failure.set(e)
            }
        }
        return AnalysisThread(thread, failure)
    }

    private companion object {
        const val TIMEOUT_MS = 30_000L
    }
}

/**
 * Registers the stop-the-world cache cleaner, which the default test configuration replaces with a no-op cleaner.
 */
private object StopWorldCacheCleanerTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.registerService(KaFirCacheCleaner::class.java, KaFirStopWorldCacheCleaner(project))
    }
}
