/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.topics

import org.jetbrains.kotlin.analysis.project.structure.KtModule

public fun interface KotlinGlobalModuleStateModificationListener {
    /**
     * [onModification] is invoked in a write action before or after global module state modification.
     *
     * The module structure, source code, and binary content of all [KtModule]s in the project should be considered modified when this event
     * is received. This includes source files being moved or removed, binary content being added, removed, or changed, and modules possibly
     * being removed. Thus, all caches related to module structure, source code, and binaries should be invalidated.
     *
     * @see KotlinTopics
     */
    public fun onModification()
}
