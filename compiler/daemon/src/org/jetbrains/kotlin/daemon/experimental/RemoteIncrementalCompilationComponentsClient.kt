/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental

import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.daemon.common.experimental.CompilerCallbackServicesFacadeClientSide
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.TargetId

class RemoteIncrementalCompilationComponentsClient(val facade: CompilerCallbackServicesFacadeClientSide, val profiler: Profiler = DummyProfiler()) : IncrementalCompilationComponents {
    override fun getIncrementalCache(target: TargetId): IncrementalCache = RemoteIncrementalCacheClient(facade, target, profiler)
}
