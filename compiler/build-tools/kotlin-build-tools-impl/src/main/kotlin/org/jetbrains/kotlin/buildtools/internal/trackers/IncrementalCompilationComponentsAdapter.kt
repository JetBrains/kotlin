/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.trackers

import org.jetbrains.kotlin.buildtools.api.jvm.CompilerIncrementalCompilationComponents
import org.jetbrains.kotlin.buildtools.api.jvm.CompilerTargetId
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.TargetId

internal class IncrementalCompilationComponentsAdapter(
    private val components: CompilerIncrementalCompilationComponents,
) : IncrementalCompilationComponents {
    override fun getIncrementalCache(target: TargetId): IncrementalCache =
        IncrementalCacheAdapter(components.getIncrementalCache(CompilerTargetId(target.name, target.type)))
}
