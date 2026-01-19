/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

import com.intellij.util.messages.Topic
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus

/**
 * In the Analysis API, [KotlinModificationEvent]s signal changes in source code, module and project settings, or project structure. These
 * events should be subscribed to and published via the [TOPIC] on the [Analysis API message bus][analysisMessageBus]. They must be
 * published in a **write action**.
 *
 * The specifics of the modification are determined by the type of [KotlinModificationEvent]:
 *
 * - [KotlinModuleStateModificationEvent]: Module settings or module structure changes for a specific module (e.g. module update/removal).
 * - [KotlinModuleOutOfBlockModificationEvent]: Out-of-block modification in the source code of a specific module.
 * - [KotlinGlobalModuleStateModificationEvent]: Global changes in project settings or project structure.
 * - [KotlinGlobalSourceModuleStateModificationEvent]: Global changes in source module settings or structure.
 * - [KotlinGlobalScriptModuleStateModificationEvent]: Global changes in script module settings or structure.
 * - [KotlinGlobalSourceOutOfBlockModificationEvent]: Global out-of-block modification potentially affecting all source modules.
 * - [KotlinCodeFragmentContextModificationEvent]: Changes to the context of code fragments depending on a specific module.
 *
 * Care needs to be taken with the lack of interplay between different types of events: Publishing a global modification event, for example,
 * does not imply the corresponding module-level event. Similarly, publishing a module state modification event does not imply out-of-block
 * modification.
 *
 * Global modification events are published when it's not feasible or desired to publish events for a single module, or a limited set of
 * modules. For example, a change in the environment such as removing an SDK might affect all modules, so a global event is more
 * appropriate.
 *
 * ### Timing Guarantees
 *
 * All modification events are guaranteed to be published in the write action in which the modification happens. Beyond that, the exact
 * timing is not strictly defined for most modification events.
 *
 * Most modification events may be published before or after a modification, so subscribers should not assume that the modification has or
 * hasn't happened yet. The reason for this design decision is that some of the underlying events (such as PSI tree changes) may be
 * published before or after a change, or even both. Modification events published before the modification should however be published close
 * to the modification.
 *
 * Only [KotlinModuleStateModificationEvent] guarantees that the event is published before the module is affected. This allows subscribers
 * to access the module's properties and dependencies to invalidate or update caches.
 *
 * ### Implementation Notes
 *
 * Analysis API platforms need to take care of publishing modification events via the [analysisMessageBus]. In general, if a platform works
 * with static code and static module structure, it does not need to publish any events. However, the contracts of the various modification
 * events need to be kept in mind. For example, if a platform can guarantee a static module structure but source code can still change,
 * module state modification events do not need to be published, but out-of-block modification events do.
 *
 * Source code modification should always be handled with [KaSourceModificationService]. It publishes out-of-block modification events if
 * it detects an out-of-block change, which makes modification handling much easier. But it also invalidates local caches on local changes,
 * which currently cannot be accomplished by modification events with the same level of granularity.
 *
 * @see KaSourceModificationLocality
 */
@KaPlatformInterface
public sealed interface KotlinModificationEvent {
    @KaPlatformInterface
    public companion object {
        /**
         * @see KotlinModificationEvent
         * @see KotlinModificationEventListener
         */
        public val TOPIC: Topic<KotlinModificationEventListener> = Topic(
            KotlinModificationEventListener::class.java,
            Topic.BroadcastDirection.TO_CHILDREN,
            true,
        )
    }
}

/**
 * A listener for [KotlinModificationEvent]s. It should be registered on the [analysisMessageBus] with [KotlinModificationEvent.TOPIC].
 */
@KaPlatformInterface
public fun interface KotlinModificationEventListener {
    /**
     * [onModification] is invoked before or after the modification and usually in a write action. However, the specific timing depends on
     * the type of [event].
     *
     * See also the KDoc of [KotlinModificationEvent] for an in-depth explanation of timing guarantees.
     */
    public fun onModification(event: KotlinModificationEvent)
}

/**
 * [KotlinModificationEventKind] represents the kinds of [KotlinModificationEvent]s. While it is not required to publish or subscribe to
 * modification events, it can be useful when abstracting over modification events in general, for example in tests.
 */
@KaPlatformInterface
public enum class KotlinModificationEventKind {
    MODULE_STATE_MODIFICATION,
    MODULE_OUT_OF_BLOCK_MODIFICATION,
    GLOBAL_MODULE_STATE_MODIFICATION,
    GLOBAL_SOURCE_MODULE_STATE_MODIFICATION,
    GLOBAL_SCRIPT_MODULE_STATE_MODIFICATION,
    GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION,
    CODE_FRAGMENT_CONTEXT_MODIFICATION,
}

@KaPlatformInterface
public val KotlinModificationEventKind.isModuleLevel: Boolean
    get() = this == KotlinModificationEventKind.MODULE_STATE_MODIFICATION ||
            this == KotlinModificationEventKind.MODULE_OUT_OF_BLOCK_MODIFICATION ||
            this == KotlinModificationEventKind.CODE_FRAGMENT_CONTEXT_MODIFICATION

@KaPlatformInterface
public val KotlinModificationEventKind.isGlobalLevel: Boolean
    get() = !isModuleLevel
