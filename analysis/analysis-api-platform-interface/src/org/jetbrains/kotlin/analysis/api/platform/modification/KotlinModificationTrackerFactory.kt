/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
 * In K2 mode, the modification trackers created by this factory must be incremented on specific [KotlinModificationEvent]s. In that sense,
 * they can be viewed as a facade for modification events, for the use case when there are many, possibly short-lived objects that need to
 * be notified of a change. In such cases, listener and subscriber management has too much overhead, making modification trackers the
 * preferred solution.
 *
 * ### Implementation Notes
 *
 * [KotlinModificationTrackerByEventFactoryBase] can be inherited from to implement this platform component based on published modification
 * events.
 */
public interface KotlinModificationTrackerFactory : KotlinPlatformComponent {
    /**
     * Creates an out-of-block [ModificationTracker] which is incremented every time there is an out-of-block change in one of the project's
     * source modules.
     *
     * See [KotlinModificationEvent] for a definition of out-of-block modification.
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
 * See [KotlinModificationEvent] for a definition of out-of-block modification.
 */
public fun Project.createProjectWideOutOfBlockModificationTracker(): ModificationTracker =
    KotlinModificationTrackerFactory.getInstance(this).createProjectWideOutOfBlockModificationTracker()

/**
 * Creates a [ModificationTracker] which is incremented every time libraries in the project are changed.
 */
public fun Project.createAllLibrariesModificationTracker(): ModificationTracker =
    KotlinModificationTrackerFactory.getInstance(this).createLibrariesWideModificationTracker()
