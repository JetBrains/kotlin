/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import java.util.Objects

/**
 * This event signals that [module]'s settings or structure are changing. The event is published in a write action *before* the [module] is
 * updated or removed (see [KotlinModuleStateModificationKind] for specifics). This allows subscribers to access the module's properties and
 * dependencies to invalidate or update caches.
 *
 * See [KotlinModificationEvent] for important contracts common to all modification events.
 */
public class KotlinModuleStateModificationEvent(
    public val module: KaModule,
    public val modificationKind: KotlinModuleStateModificationKind,
) : KotlinModificationEvent {
    override fun equals(other: Any?): Boolean =
        this === other ||
                other is KotlinModuleStateModificationEvent && module == other.module && modificationKind == other.modificationKind

    override fun hashCode(): Int = Objects.hash(module, modificationKind)
}

/**
 * Describes the kind of module state modification affecting a [KaModule] in more detail.
 */
public enum class KotlinModuleStateModificationKind {
    /**
     * The [KaModule]'s properties or references to other modules are being changed.
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
     * The [KaModule] is being removed. Because this event is published before the removal, the [KaModule] can still be accessed to clear
     * caches. It should be removed from any caches managed by the subscriber to avoid stale or broken keys/values.
     */
    REMOVAL,
}
