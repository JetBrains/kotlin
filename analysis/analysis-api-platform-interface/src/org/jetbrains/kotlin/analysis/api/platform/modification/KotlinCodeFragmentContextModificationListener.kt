/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

public fun interface KotlinCodeFragmentContextModificationListener {
    /**
     * [onModification] is invoked in a write action before or after a context change for code fragments depending on the [module].
     *
     * All code fragments depending on [module], both directly or transitively, should be considered modified when this event is received.
     *
     * @see KotlinModificationTopics
     */
    public fun onModification(module: KaModule)
}