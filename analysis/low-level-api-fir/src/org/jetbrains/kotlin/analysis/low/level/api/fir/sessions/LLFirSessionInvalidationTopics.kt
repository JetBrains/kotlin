/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.util.messages.Topic
import org.jetbrains.kotlin.analysis.project.structure.KtModule

/**
 * [Topic]s for events published by [LLFirSessionInvalidationService] *after* session invalidation. These topics should be subscribed to via
 * the Analysis API message bus: [analysisMessageBus][org.jetbrains.kotlin.analysis.providers.analysisMessageBus].
 *
 * Session invalidation events are guaranteed to be published after the associated sessions have been invalidated. Because sessions are
 * invalidated in a write action, all session invalidation events are published during that same write action.
 *
 * When a session is garbage-collected due to being softly reachable, no session invalidation event will be published for it. See the
 * documentation of [LLFirSession] for background information.
 *
 * Session invalidation events are not published for unstable
 * [KtDanglingFileModules][org.jetbrains.kotlin.analysis.project.structure.KtDanglingFileModule].
 */
object LLFirSessionInvalidationTopics {
    val SESSION_INVALIDATION: Topic<LLFirSessionInvalidationListener> =
        Topic(LLFirSessionInvalidationListener::class.java, Topic.BroadcastDirection.TO_CHILDREN, true)
}

interface LLFirSessionInvalidationListener {
    /**
     * [afterInvalidation] is published when sessions for the given [modules] have been invalidated. Because the sessions are already
     * invalid, the event carries their [KtModule][org.jetbrains.kotlin.analysis.project.structure.KtModule]s.
     *
     * @see LLFirSessionInvalidationTopics
     */
    fun afterInvalidation(modules: Set<KtModule>)

    /**
     * [afterGlobalInvalidation] is published when all sessions may have been invalidated. The event doesn't guarantee that all sessions
     * have been invalidated, but e.g. caches should be cleared as if this was the case. This event is published when the invalidated
     * sessions cannot be easily enumerated.
     *
     * @see LLFirSessionInvalidationTopics
     */
    fun afterGlobalInvalidation()
}
