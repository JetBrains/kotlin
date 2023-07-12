/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.topics

import org.jetbrains.kotlin.analysis.project.structure.KtModule

public fun interface KotlinGlobalModuleStateModificationListener {
    /**
     * [onModification] is invoked in a write action before or after global modification of the module state of all [KtModule]s.
     *
     * This event is published after SDK removal and to invalidate caches during/between tests.
     */
    public fun onModification()
}
