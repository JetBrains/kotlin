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

import org.jetbrains.kotlin.config.IncrementalCompilation
import java.io.File

internal const val STANDALONE_CACHE_VERSION = 0
internal const val STANDALONE_VERSION_FILE_NAME = "standalone-ic-format-version.txt"

internal fun standaloneCacheVersion(dataRoot: File): CacheVersion =
        CacheVersion(ownVersion = STANDALONE_CACHE_VERSION,
                versionFile = File(dataRoot, STANDALONE_VERSION_FILE_NAME),
                whenVersionChanged = CacheVersion.Action.REBUILD_ALL_KOTLIN,
                whenTurnedOn = CacheVersion.Action.REBUILD_ALL_KOTLIN,
                whenTurnedOff = CacheVersion.Action.REBUILD_ALL_KOTLIN,
                isEnabled = { IncrementalCompilation.isExperimental() })
