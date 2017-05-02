/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.kotlin.incremental.components

import java.io.Serializable

data class JvmPackagePartProto(val data: ByteArray, val strings: Array<String>) : Serializable

interface IncrementalCache {
    fun getObsoletePackageParts(): Collection<String>

    fun getObsoleteMultifileClasses(): Collection<String>

    fun getStableMultifileFacadeParts(facadeInternalName: String): Collection<String>?

    fun getPackagePartData(partInternalName: String): JvmPackagePartProto?

    fun getModuleMappingData(): ByteArray?

    fun getClassFilePath(internalClassName: String): String

    fun close()
}
