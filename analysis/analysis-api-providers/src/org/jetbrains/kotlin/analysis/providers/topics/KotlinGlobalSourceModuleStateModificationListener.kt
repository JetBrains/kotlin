/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.topics

import org.jetbrains.kotlin.analysis.project.structure.KtModule

public fun interface KotlinGlobalSourceModuleStateModificationListener {
    /**
     * [onModification] is invoked in a write action before or after global source module state modification.
     *
     * The module structure and source code of all source [KtModule]s in the project should be considered modified when this event is
     * received. This includes source files being moved or removed, and source modules possibly being removed. Thus, all caches related to
     * source module structure and source code should be invalidated.
     *
     * Library modules (including library sources) do not need to be considered modified, so any caches related to library modules and their
     * contents may be kept.
     *
     * @see KotlinTopics
     */
    public fun onModification()
}
