/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common

import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.TargetId

class RemoteIncrementalCompilationComponentsClient(
    @Suppress("DEPRECATION") val facade: CompilerCallbackServicesFacade,
    val profiler: Profiler = DummyProfiler()
) : IncrementalCompilationComponents {
    override fun getIncrementalCache(target: TargetId): IncrementalCache = RemoteIncrementalCacheClient(facade, target, profiler)
}
