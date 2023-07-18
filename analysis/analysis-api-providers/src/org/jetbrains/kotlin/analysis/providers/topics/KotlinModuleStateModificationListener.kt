/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.topics

import org.jetbrains.kotlin.analysis.project.structure.KtModule

public fun interface KotlinModuleStateModificationListener {
    /**
     * [onModification] is invoked in a write action *before* the [module] is updated or removed (see [modificationKind] for specifics).
     *
     * @see KotlinTopics
     */
    public fun onModification(module: KtModule, modificationKind: KotlinModuleStateModificationKind)
}

public enum class KotlinModuleStateModificationKind {
    /**
     * The [KtModule]'s properties or references to other modules are being changed.
     *
     * #### Examples
     *
     *  - The name of the module is being changed.
     *  - The module's content roots are being changed, such as adding another source folder to a source module.
     *  - If module A depends on module B and module B is being removed, in addition to the removal event for module B, module A also
     *    receives an update event.
     */
    UPDATE,

    /**
     * The [KtModule] is being removed. Because this event is published before the removal, the [KtModule] can still be accessed to clear
     * caches. It should be removed from any caches managed by the subscriber to avoid stale or broken keys/values.
     */
    REMOVAL,
}
