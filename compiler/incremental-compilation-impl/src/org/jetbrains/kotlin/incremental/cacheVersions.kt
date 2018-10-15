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

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.storage.version.CacheVersionManager
import org.jetbrains.kotlin.incremental.storage.version.localCacheVersionManager
import org.jetbrains.kotlin.incremental.storage.version.lookupsCacheVersionManager
import java.io.File

internal const val STANDALONE_CACHE_VERSION = 2
internal const val STANDALONE_VERSION_FILE_NAME = "standalone-ic-format-version.txt"

fun standaloneCacheVersionManager(dataRoot: File, enabled: Boolean): CacheVersionManager =
    customCacheVersionManager(STANDALONE_CACHE_VERSION, STANDALONE_VERSION_FILE_NAME, dataRoot, enabled)

fun customCacheVersionManager(version: Int, fileName: String, dataRoot: File, enabled: Boolean): CacheVersionManager =
    CacheVersionManager(
        File(dataRoot, fileName),
        if (enabled) version else null
    )

fun commonCacheVersionsManagers(cachesDir: File, enabled: Boolean): List<CacheVersionManager> =
    listOf(
        localCacheVersionManager(cachesDir, enabled),
        lookupsCacheVersionManager(cachesDir, enabled)
    )