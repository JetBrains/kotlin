/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analysis.providers.topics.KotlinTopics

/**
 * [KotlinModificationTrackerFactory] creates modification trackers for select modification events.
 *
 * Further modification tracking is implemented with a subscription-based mechanism via [KotlinTopics]. Modification trackers make the most
 * sense when there are many, possibly short-lived objects that need to be notified of a change. In such cases, subscriber management in the
 * message bus adds too much overhead.
 */
public abstract class KotlinModificationTrackerFactory {
    /**
     * Creates an out-of-block modification tracker which is incremented every time there is an out-of-block change in some source project
     * module.
     *
     * ### Out-of-block Modification (OOBM)
     *
     * Out-of-block modification is a source code modification which may change the resolution of other non-local declarations.
     *
     * #### Example 1
     *
     * ```
     * val x = 10<caret>
     * val z = x
     * ```
     *
     * If we change the initializer of `x` to `"str"` the return type of `x` will become `String` instead of the initial `Int`. This will
     * change the return type of `z` as it does not have an explicit type. So, it is an **OOBM**.
     *
     * #### Example 2
     *
     * ```
     * val x: Int = 10<caret>
     * val z = x
     * ```
     *
     * If we change `10` to `"str"` as in the first example, it would not change the type of `z`, so it is not an **OOBM**.
     *
     * #### Examples of source code modifications which result in an **OOBM**
     *
     *  - Modification inside non-local (i.e. accessible outside) declaration without explicit return type specified
     *  - Modification of a package
     *  - Creation of a new declaration
     *  - Moving a declaration to another package
     *
     * Generally, all modifications which happen outside the body of a callable declaration (functions, accessors, or properties) with an
     * explicit type are considered **OOBM**.
     *
     * @see ModificationTracker
     */
    public abstract fun createProjectWideOutOfBlockModificationTracker(): ModificationTracker

    /**
     * Creates a modification tracker which is incremented every time libraries in the project are changed.
     *
     * @see ModificationTracker
     */
    public abstract fun createLibrariesWideModificationTracker(): ModificationTracker

    public companion object {
        public fun getInstance(project: Project): KotlinModificationTrackerFactory =
            project.getService(KotlinModificationTrackerFactory::class.java)
    }
}

/**
 * Creates an **OOBM** tracker which is incremented every time there is an OOB change in some source project module.
 *
 * See [KotlinModificationTrackerFactory.createProjectWideOutOfBlockModificationTracker] for the definition of **OOBM**.
 * @see ModificationTracker
 */
public fun Project.createProjectWideOutOfBlockModificationTracker(): ModificationTracker =
    KotlinModificationTrackerFactory.getInstance(this).createProjectWideOutOfBlockModificationTracker()

/**
 * Creates a modification tracker which is incremented every time libraries in the project are changed.
 *
 * See [KotlinModificationTrackerFactory] for the definition of **OOBM**.
 * @see ModificationTracker
 */
public fun Project.createAllLibrariesModificationTracker(): ModificationTracker =
    KotlinModificationTrackerFactory.getInstance(this).createLibrariesWideModificationTracker()
