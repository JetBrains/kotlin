/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

public fun interface KotlinModuleOutOfBlockModificationListener {
    /**
     * [onModification] is invoked in a write action before or after an out-of-block modification happens in [module]'s source code.
     *
     * This event may be published for any and all source code changes, not just out-of-block modifications, to simplify the implementation
     * of modification detection.
     *
     * See [KotlinModificationTopics] for an explanation of out-of-block modifications.
     */
    public fun onModification(module: KaModule)
}
