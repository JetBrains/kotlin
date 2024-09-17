/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptDependencyModule

public fun interface KotlinGlobalScriptModuleStateModificationListener {
    /**
     * [onModification] is invoked in a write action before or after global script state modification.
     *
     * The module structure, source code, and binary content of all [KaScriptModule]s and [KaScriptDependencyModule]s in the project
     * should be considered modified when this event is received.
     *
     * Thus, all caches related to kotlin scripts module structure, source code, and binaries should be invalidated.
     *
     * @see KotlinModificationTopics
     */
    public fun onModification()
}
