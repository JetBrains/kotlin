/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule


public abstract class KotlinModificationTrackerFactory {
    /**
     * Creates **OOBM** tracker which is incremented every time there is **OOB** change in some source project module.
     *
     * See [createModuleWithoutDependenciesOutOfBlockModificationTracker] for the definition of **OOBM**.
     * @see ModificationTracker
     */
    public abstract fun createProjectWideOutOfBlockModificationTracker(): ModificationTracker

    /**
     * Creates **OOBM** tracker which is incremented every time there is OOB change in the [module].
     * This tracker should not consider changes in dependent modules.
     * If you do not need incrementalliy in your tool (eg, it is okay to reanalyse the whole worked on every source code change) consider incrementing returned tracker on every source code change.
     * If tour tool works with static code (so no codebase changes in possible) just return static modification tracker here.
     *
     * **Out Of Block Modification (OOBM)** is a such modification in source code which may change resolve of other non-local declarations.
     *
     * Consider the following example #1:
     * ```
     * val x = 10<caret>
     * val z = x
     * ```
     * If we change initializer of `x` to `"str"` the return type of `x` will become 'String' instead of initial `Int`.
     * This will change the return type of `z` as it does not have explicit type specified. So, it is an **OOBM**.
     *
     *
     * Consider example #2:
     *  ```
     * val x: Int = 10<caret>
     * val z = x
     * ```
     * If we change 10 to "str" as in example #1, it would not change type of z, so it is not **OOBM**.
     *
     * Example of modifications in source code, which results **OOBM**
     * - modification inside non-local (ie, accessible outside) declaration without explicit return type specified
     * - modification o a package
     * - creating new declaration
     * - moving declaration to another package
     *
     * Generally, all modifications which happens outside callable declaration (function, accessor or property) body with explicit type are considered **OOBM**
     * @see ModificationTracker
     */
    public abstract fun createModuleWithoutDependenciesOutOfBlockModificationTracker(module: KtSourceModule): ModificationTracker

    /**
     * Creates modification tracker which is incremented every time libraries in project are changed.
     *
     * See [KotlinModificationTrackerFactory] for the definition of **OOBM**.
     * @see ModificationTracker
     */
    public abstract fun createLibrariesModificationTracker(): ModificationTracker

    @TestOnly
    public abstract fun incrementModificationsCount()
}

/**
 * Creates **OOBM** tracker which is incremented every time there is OOB change in some source project module.
 *
 * See [KotlinModificationTrackerFactory.createModuleWithoutDependenciesOutOfBlockModificationTracker] for the definition of **OOBM**.
 * @see ModificationTracker
 */
public fun Project.createProjectWideOutOfBlockModificationTracker(): ModificationTracker =
    ServiceManager.getService(this, KotlinModificationTrackerFactory::class.java)
        .createProjectWideOutOfBlockModificationTracker()

/**
 * Creates **OOBM** tracker which is incremented every time there is OOB change in the [module].
 *
 * See [KotlinModificationTrackerFactory.createModuleWithoutDependenciesOutOfBlockModificationTracker] for the definition of **OOBM**.
 * @see ModificationTracker
 */
public fun KtSourceModule.createModuleWithoutDependenciesOutOfBlockModificationTracker(project: Project): ModificationTracker =
    ServiceManager.getService(project, KotlinModificationTrackerFactory::class.java)
        .createModuleWithoutDependenciesOutOfBlockModificationTracker(this)

/**
 * Creates modification tracker which is incremented every time libraries in project are changed.
 *
 * See [KotlinModificationTrackerFactory] for the definition of **OOBM**.
 * @see ModificationTracker
 */
public fun Project.createLibrariesModificationTracker(): ModificationTracker =
    ServiceManager.getService(this, KotlinModificationTrackerFactory::class.java)
        .createLibrariesModificationTracker()
