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

package org.jetbrains.kotlin.rmi.service

import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.rmi.CompileService

public class RemoteIncrementalCacheClient(val cache: CompileService.RemoteIncrementalCache): IncrementalCache {
    override fun getObsoletePackageParts(): Collection<String> = cache.getObsoletePackageParts()

    override fun getPackagePartData(fqName: String): JvmPackagePartProto? = cache.getPackagePartData(fqName)

    override fun getModuleMappingData(): ByteArray? = cache.getModuleMappingData()

    override fun registerInline(fromPath: String, jvmSignature: String, toPath: String) {
        cache.registerInline(fromPath, jvmSignature, toPath)
    }

    override fun getClassFilePath(internalClassName: String): String = cache.getClassFilePath(internalClassName)

    override fun close(): Unit = cache.close()
}
