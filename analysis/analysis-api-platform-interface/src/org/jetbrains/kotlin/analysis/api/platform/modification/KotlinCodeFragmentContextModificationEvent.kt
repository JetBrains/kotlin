/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * This event signals that the context of any code fragment depending on [module] is changing. All code fragments depending on [module],
 * both directly or transitively, should be considered modified when this event is received.
 *
 * See [KotlinModificationEvent] for important contracts common to all modification events.
 */
public class KotlinCodeFragmentContextModificationEvent(public val module: KaModule) : KotlinModificationEvent {
    override fun equals(other: Any?): Boolean =
        this === other || other is KotlinCodeFragmentContextModificationEvent && module == other.module

    override fun hashCode(): Int = module.hashCode()
}
