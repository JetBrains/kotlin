/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.common.CompilerCallbackServicesFacade
import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.modules.TargetId

class RemoteIncrementalCacheClient(val facade: CompilerCallbackServicesFacade, val target: TargetId, val profiler: Profiler = DummyProfiler()): IncrementalCache {

    override fun getObsoletePackageParts(): Collection<String> = profiler.withMeasure(this) { facade.incrementalCache_getObsoletePackageParts(target) }

    override fun getObsoleteMultifileClasses(): Collection<String> = profiler.withMeasure(this) { facade.incrementalCache_getObsoleteMultifileClassFacades(target) }

    override fun getStableMultifileFacadeParts(facadeInternalName: String): Collection<String>? = profiler.withMeasure(this) { facade.incrementalCache_getMultifileFacadeParts(target, facadeInternalName) }

    override fun getPackagePartData(partInternalName: String): JvmPackagePartProto? = profiler.withMeasure(this) { facade.incrementalCache_getPackagePartData(target, partInternalName) }

    override fun getModuleMappingData(): ByteArray? = profiler.withMeasure(this) { facade.incrementalCache_getModuleMappingData(target) }

    override fun getClassFilePath(internalClassName: String): String = profiler.withMeasure(this) { facade.incrementalCache_getClassFilePath(target,internalClassName) }

    override fun close(): Unit = profiler.withMeasure(this) { facade.incrementalCache_close(target) }
}
