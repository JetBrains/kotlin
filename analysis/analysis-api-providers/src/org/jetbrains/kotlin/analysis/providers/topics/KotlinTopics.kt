/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.topics

import com.intellij.util.messages.Topic
import org.jetbrains.kotlin.analysis.providers.analysisMessageBus

/**
 * [Topic]s for Analysis API event subscription and publication. These topics should be subscribed to and published to via the Analysis API
 * message bus: [analysisMessageBus].
 *
 * See the individual listener interfaces for documentation about the events described by these topics:
 *
 *  - [KotlinModuleStateModificationListener]
 *  - [KotlinModuleOutOfBlockModificationListener]
 *  - [KotlinGlobalModuleStateModificationListener]
 *  - [KotlinGlobalSourceModuleStateModificationListener]
 *  - [KotlinGlobalSourceOutOfBlockModificationListener]
 *
 * Care needs to be taken with the lack of interplay between different types of topics: Publishing a global modification event, for example,
 * does not imply the corresponding module-level event. Similarly, publishing a module state modification event does not imply out-of-block
 * modification.
 *
 * Global modification events are published when it's not feasible or desired to publish events for a single module, or a limited set of
 * modules. For example, a change in the environment such as removing an SDK might affect all modules, so a global event is more
 * appropriate.
 *
 * #### Timing Guarantees
 *
 * Most modification events may be published before or after a modification, so subscribers should not assume that the modification has or
 * hasn't happened yet. The reason for this design decision is that some of the underlying events (such as PSI tree changes) may be
 * published before and after a change, or even both. Modification events published before the modification should however be published
 * close to the modification.
 *
 * Only [module state modification events][KotlinModuleStateModificationListener] guarantee that the event is published before the module is
 * affected. This allows subscribers to access the module's properties and dependencies to invalidate or update caches.
 *
 * #### Implementation Notes
 *
 * Analysis API implementations need to take care of publishing to these topics via the [analysisMessageBus]. In general, if your tool works
 * with static code and static module structure, you do not need to publish any events. However, keep in mind the contracts of the various
 * topics. For example, if your tool can guarantee a static module structure but source code can still change, module state modification
 * events do not need to be published, but out-of-block modification events do.
 */
public object KotlinTopics {
    public val MODULE_STATE_MODIFICATION: Topic<KotlinModuleStateModificationListener> =
        Topic(KotlinModuleStateModificationListener::class.java, Topic.BroadcastDirection.TO_CHILDREN, true)

    public val MODULE_OUT_OF_BLOCK_MODIFICATION: Topic<KotlinModuleOutOfBlockModificationListener> =
        Topic(KotlinModuleOutOfBlockModificationListener::class.java, Topic.BroadcastDirection.TO_CHILDREN, true)

    public val GLOBAL_MODULE_STATE_MODIFICATION: Topic<KotlinGlobalModuleStateModificationListener> =
        Topic(KotlinGlobalModuleStateModificationListener::class.java, Topic.BroadcastDirection.TO_CHILDREN, true)

    public val GLOBAL_SOURCE_MODULE_STATE_MODIFICATION: Topic<KotlinGlobalSourceModuleStateModificationListener> =
        Topic(KotlinGlobalSourceModuleStateModificationListener::class.java, Topic.BroadcastDirection.TO_CHILDREN, true)

    public val GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION: Topic<KotlinGlobalSourceOutOfBlockModificationListener> =
        Topic(KotlinGlobalSourceOutOfBlockModificationListener::class.java, Topic.BroadcastDirection.TO_CHILDREN, true)
}
