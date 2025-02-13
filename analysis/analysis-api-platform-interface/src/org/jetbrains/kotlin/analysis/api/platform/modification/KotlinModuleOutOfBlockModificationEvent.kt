/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * [KotlinModuleOutOfBlockModificationEvent] signals that an out-of-block modification is occurring in the sources of [module]. The event is
 * published in a write action before or after the modification.
 *
 * This event may be published for any and all source code changes, not just out-of-block modifications, to simplify the implementation of
 * modification detection.
 *
 * See [KotlinModificationEvent] for an explanation of out-of-block modifications.
 */
public class KotlinModuleOutOfBlockModificationEvent(public val module: KaModule) : KotlinModificationEvent {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is KotlinModuleOutOfBlockModificationEvent && module == other.module
    }

    override fun hashCode(): Int = module.hashCode()
}
