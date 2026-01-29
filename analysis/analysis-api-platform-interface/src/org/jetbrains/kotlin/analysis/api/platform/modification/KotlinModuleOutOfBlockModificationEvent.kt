/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * This event signals that an [out-of-block modification][KaSourceModificationLocality.OutOfBlock] is occurring in the sources of [module].
 *
 * This event may be published for any and all source code changes, not just out-of-block modifications, to simplify the implementation of
 * modification detection.
 *
 * See [KotlinModificationEvent] for important contracts common to all modification events.
 */
@KaPlatformInterface
public class KotlinModuleOutOfBlockModificationEvent(public val module: KaModule) : KotlinModificationEvent {
    override fun equals(other: Any?): Boolean =
        this === other || other is KotlinModuleOutOfBlockModificationEvent && module == other.module

    override fun hashCode(): Int = module.hashCode()
}
