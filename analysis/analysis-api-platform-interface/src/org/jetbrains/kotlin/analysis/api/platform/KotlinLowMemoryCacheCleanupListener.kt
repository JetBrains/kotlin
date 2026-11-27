/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform

import com.intellij.util.messages.Topic
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface

/**
 * A listener for the low-memory cache cleanup of the Analysis API, which invalidates all analysis caches when the platform runs low on
 * memory.
 *
 * The cleanup cannot invalidate caches while an [analyze][org.jetbrains.kotlin.analysis.api.session.analyze] block is running. If analysis
 * is ongoing when the cleanup is requested, the cleanup is *deferred* until the last ongoing analysis finishes, and new analyses must wait
 * until the cleanup is complete. The listener allows the platform to collect statistics about these cleanups and the time spent waiting for
 * them, for example to report usage statistics.
 *
 * The events are published to the [analysisMessageBus] of the project. They are published synchronously on the thread which performed the
 * cleanup or which waited for it, so listener implementations must be fast and must not block. They must not start analysis either.
 * Exceptions thrown by the listener are logged and don't affect the cleanup.
 *
 * Whether the cleanup is enabled depends on the platform. If it is disabled, no events are published.
 */
@KaPlatformInterface
public interface KotlinLowMemoryCacheCleanupListener {
    /**
     * This event is published after the caches have been invalidated.
     *
     * For a deferred cleanup, the event is published after the analyses waiting for the cleanup have been allowed to continue. Their
     * [afterWaitingForCleanup] events may be published before or after this event.
     *
     * @param invalidationDurationMs The time taken by cache invalidation itself.
     * @param requestToCompletionDurationMs The time between the cleanup request and the completion of the cleanup. For a deferred
     *  cleanup, this includes the time spent waiting for ongoing analyses to finish.
     * @param isDeferred Whether the cleanup had to wait for ongoing analyses to finish.
     */
    public fun afterCleanup(invalidationDurationMs: Long, requestToCompletionDurationMs: Long, isDeferred: Boolean)

    /**
     * This event is published after a thread which wanted to start analysis has finished waiting for a deferred cleanup.
     *
     * Nested analyses don't wait for the cleanup, so they don't publish this event.
     *
     * @param waitDurationMs The time the thread spent waiting.
     * @param outcome Why the thread finished waiting.
     */
    public fun afterWaitingForCleanup(waitDurationMs: Long, outcome: WaitOutcome)

    /**
     * Why a thread finished waiting for a deferred cleanup (see [afterWaitingForCleanup]).
     */
    @KaPlatformInterface
    public enum class WaitOutcome {
        /**
         * The cleanup has been completed.
         */
        CLEANUP_COMPLETED,

        /**
         * The thread finished waiting before the cleanup was completed, because none of the threads with ongoing analysis were running. This
         * suggests that an ongoing analysis is waiting for the current thread, which would otherwise cause a deadlock (see KT-76577).
         */
        DEADLOCK_SUSPECTED,

        /**
         * The wait ended with an exception, usually because the thread's operation was canceled.
         */
        CANCELED,
    }

    @KaPlatformInterface
    public companion object {
        /**
         * The topic of [KotlinLowMemoryCacheCleanupListener] events, which are published to the project's [analysisMessageBus].
         */
        public val TOPIC: Topic<KotlinLowMemoryCacheCleanupListener> = Topic(
            KotlinLowMemoryCacheCleanupListener::class.java,
            Topic.BroadcastDirection.TO_CHILDREN,
            true,
        )
    }
}
