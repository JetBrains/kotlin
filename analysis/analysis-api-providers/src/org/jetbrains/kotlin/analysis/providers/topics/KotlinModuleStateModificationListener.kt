/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.topics

import org.jetbrains.kotlin.analysis.project.structure.KtModule

public fun interface KotlinModuleStateModificationListener {
    /**
     * [onModification] is invoked in a write action before or after the [module] is moved, removed, or its structure is changed.
     * [isRemoval] signals if the event is a removal.
     */
    public fun onModification(module: KtModule, isRemoval: Boolean)
}
