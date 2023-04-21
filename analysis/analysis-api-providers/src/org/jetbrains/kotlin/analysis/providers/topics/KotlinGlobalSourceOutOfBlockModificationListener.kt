/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.topics

import org.jetbrains.kotlin.analysis.project.structure.KtModule

public fun interface KotlinGlobalSourceOutOfBlockModificationListener {
    /**
     * [afterModification] is invoked in a write action after global out-of-block modification of all source [KtModule]s, excluding binary
     * modules.
     *
     * This event is published on global PSI changes.
     */
    public fun afterModification()
}
