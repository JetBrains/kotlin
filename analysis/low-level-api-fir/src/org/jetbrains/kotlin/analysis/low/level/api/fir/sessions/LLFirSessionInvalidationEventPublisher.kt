/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.project.structure.KtDanglingFileModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.isStable
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus

/**
 * [LLFirSessionInvalidationEventPublisher] publishes [session invalidation events][LLFirSessionInvalidationTopics] after session
 * invalidation to allow caches that depend on [LLFirSession]s to be invalidated actively. These events are not published after garbage
 * collection of softly reachable sessions. See [LLFirSession] for more information.
 */
internal class LLFirSessionInvalidationEventPublisher(private val project: Project) {
    /**
     * [invalidatedModules] can only exist during write actions while executing [collectSessionsAndPublishInvalidationEvent], so we don't
     * have to use a thread-safe collection.
     */
    private var invalidatedModules: MutableSet<KtModule>? = null

    /**
     * Invokes [action] and collects all sessions which were invalidated during its execution. At the end, publishes a session invalidation
     * event if at least one session was invalidated.
     *
     * Invalidated sessions are tracked via [collectSession].
     *
     * Must be called in a write action.
     */
    inline fun collectSessionsAndPublishInvalidationEvent(action: () -> Unit) {
        require(invalidatedModules == null) {
            "The set of invalidated modules should be `null` when `collectSessionsAndPublishInvalidationEvent` has just been called."
        }
        invalidatedModules = mutableSetOf()

        try {
            action()

            if (invalidatedModules?.isNotEmpty() == true) {
                project.analysisMessageBus
                    .syncPublisher(LLFirSessionInvalidationTopics.SESSION_INVALIDATION)
                    .afterInvalidation(invalidatedModules!!)
            }
        } finally {
            invalidatedModules = null
        }
    }

    fun collectSession(session: LLFirSession) {
        // We don't want to collect any modules outside `collectSessionsAndPublishInvalidationEvent`. For example, this might happen during
        // global invalidation, or when unstable dangling file sessions are replaced during `LLFirSessionCache.getSession`.
        val invalidatedModules = this.invalidatedModules ?: return

        // Session invalidation events don't need to be published for unstable dangling file modules.
        val ktModule = session.ktModule
        if (ktModule is KtDanglingFileModule && !ktModule.isStable) {
            return
        }

        invalidatedModules.add(ktModule)
    }

    companion object {
        fun getInstance(project: Project): LLFirSessionInvalidationEventPublisher =
            project.getService(LLFirSessionInvalidationEventPublisher::class.java)
    }
}
