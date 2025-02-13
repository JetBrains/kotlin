/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * [KotlinCodeFragmentContextModificationEvent] signals that the context of any code fragment depending on [module] is changing. The event
 * is published in a write action before or after the context change.
 *
 * All code fragments depending on [module], both directly or transitively, should be considered modified when this event is received.
 */
public class KotlinCodeFragmentContextModificationEvent(public val module: KaModule) : KotlinModificationEvent {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is KotlinCodeFragmentContextModificationEvent && module == other.module
    }

    override fun hashCode(): Int = module.hashCode()
}
