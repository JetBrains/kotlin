/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent

/**
 * [KotlinModificationTrackerFactory] creates modification trackers for select modification events.
 *
 * Further modification tracking is implemented with a subscription-based mechanism based on [KotlinModificationTopics]. Modification
 * trackers make the most sense when there are many, possibly short-lived objects that need to be notified of a change. In such cases,
 * subscriber management in the message bus adds too much overhead.
 */
public interface KotlinModificationTrackerFactory : KotlinPlatformComponent {
    /**
     * Creates an out-of-block [ModificationTracker] which is incremented every time there is an out-of-block change in one of the project's
     * source modules.
     *
     * See [KotlinModificationTopics] for a definition of out-of-block modification.
     */
    public fun createProjectWideOutOfBlockModificationTracker(): ModificationTracker

    /**
     * Creates a [ModificationTracker] which is incremented every time libraries in the project are changed.
     */
    public fun createLibrariesWideModificationTracker(): ModificationTracker

    public companion object {
        public fun getInstance(project: Project): KotlinModificationTrackerFactory = project.service()
    }
}

/**
 * Creates an out-of-block [ModificationTracker] which is incremented every time there is an out-of-block change in one of the project's
 * source modules.
 *
 * See [KotlinModificationTopics] for a definition of out-of-block modification.
 */
public fun Project.createProjectWideOutOfBlockModificationTracker(): ModificationTracker =
    KotlinModificationTrackerFactory.getInstance(this).createProjectWideOutOfBlockModificationTracker()

/**
 * Creates a [ModificationTracker] which is incremented every time libraries in the project are changed.
 */
public fun Project.createAllLibrariesModificationTracker(): ModificationTracker =
    KotlinModificationTrackerFactory.getInstance(this).createLibrariesWideModificationTracker()
