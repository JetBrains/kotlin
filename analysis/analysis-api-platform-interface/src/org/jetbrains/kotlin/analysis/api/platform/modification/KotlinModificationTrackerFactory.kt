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
 * [KotlinModificationTrackerFactory] creates modification trackers for sources and libraries.
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
     * Creates a [ModificationTracker] which is incremented every time a Kotlin source file is affected by a modification, in any of the
     * project's source modules.
     *
     * Such a modification can be any out-of-block code or project structure change affecting the analyzed source code. See
     * [KotlinModificationEvent] for a definition of out-of-block modification.
     */
    public fun createProjectWideSourceModificationTracker(): ModificationTracker

    /**
     * Creates a [ModificationTracker] which is incremented every time a library in the project is changed.
     */
    public fun createProjectWideLibraryModificationTracker(): ModificationTracker

    public companion object {
        public fun getInstance(project: Project): KotlinModificationTrackerFactory = project.service()
    }
}

/**
 * Creates a [ModificationTracker] which is incremented every time a Kotlin source file is affected by a modification, in any of the
 * project's source modules.
 *
 * Such a modification can be any out-of-block code or project structure change affecting the analyzed source code. See
 * [KotlinModificationEvent] for a definition of out-of-block modification.
 */
public fun Project.createProjectWideSourceModificationTracker(): ModificationTracker =
    KotlinModificationTrackerFactory.getInstance(this).createProjectWideSourceModificationTracker()

/**
 * Creates a [ModificationTracker] which is incremented every time a library in the project is changed.
 */
public fun Project.createProjectWideLibraryModificationTracker(): ModificationTracker =
    KotlinModificationTrackerFactory.getInstance(this).createProjectWideLibraryModificationTracker()
