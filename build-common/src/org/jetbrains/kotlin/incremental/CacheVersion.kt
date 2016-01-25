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

package org.jetbrains.kotlin.incremental

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.load.java.JvmBytecodeBinaryVersion
import org.jetbrains.kotlin.load.kotlin.JvmMetadataVersion
import java.io.File

private val NORMAL_VERSION = 8
private val EXPERIMENTAL_VERSION = 3
private val DATA_CONTAINER_VERSION = 1

private val NORMAL_VERSION_FILE_NAME = "format-version.txt"
private val EXPERIMENTAL_VERSION_FILE_NAME = "experimental-format-version.txt"
private val DATA_CONTAINER_VERSION_FILE_NAME = "data-container-format-version.txt"

class CacheVersion(
        private val ownVersion: Int,
        private val versionFile: File,
        private val whenVersionChanged: CacheVersion.Action,
        private val whenTurnedOn: CacheVersion.Action,
        private val whenTurnedOff: CacheVersion.Action,
        isEnabled: ()->Boolean
) {
    private val isEnabled by lazy(isEnabled)

    private val actualVersion: Int
        get() = versionFile.readText().toInt()

    private val expectedVersion: Int
        get() {
            val metadata = JvmMetadataVersion.INSTANCE
            val bytecode = JvmBytecodeBinaryVersion.INSTANCE
            return ownVersion * 1000000 +
                   bytecode.major * 10000 + bytecode.minor * 100 +
                   metadata.major * 1000 + metadata.minor
        }

    fun checkVersion(): Action =
            when (versionFile.exists() to isEnabled) {
                true  to true -> if (actualVersion != expectedVersion) whenVersionChanged else Action.DO_NOTHING
                false to true -> whenTurnedOn
                true  to false -> whenTurnedOff
                else -> Action.DO_NOTHING
            }

    fun saveIfNeeded() {
        if (!isEnabled) return

        if (!versionFile.parentFile.exists()) {
            versionFile.parentFile.mkdirs()
        }

        versionFile.writeText(expectedVersion.toString())
    }

    fun clean() {
        versionFile.delete()
    }

    @get:TestOnly
    val formatVersionFile: File
        get() = versionFile

    // Order of entries is important, because actions are sorted in KotlinBuilder::checkVersions
    enum class Action {
        REBUILD_ALL_KOTLIN,
        REBUILD_CHUNK,
        CLEAN_NORMAL_CACHES,
        CLEAN_EXPERIMENTAL_CACHES,
        CLEAN_DATA_CONTAINER,
        DO_NOTHING
    }
}

fun normalCacheVersion(dataRoot: File): CacheVersion =
        CacheVersion(ownVersion = NORMAL_VERSION,
                     versionFile = File(dataRoot, NORMAL_VERSION_FILE_NAME),
                     whenVersionChanged = CacheVersion.Action.REBUILD_CHUNK,
                     whenTurnedOn = CacheVersion.Action.REBUILD_CHUNK,
                     whenTurnedOff = CacheVersion.Action.CLEAN_NORMAL_CACHES,
                     isEnabled = { IncrementalCompilation.isEnabled() })

fun experimentalCacheVersion(dataRoot: File): CacheVersion =
        CacheVersion(ownVersion = EXPERIMENTAL_VERSION,
                     versionFile = File(dataRoot, EXPERIMENTAL_VERSION_FILE_NAME),
                     whenVersionChanged = CacheVersion.Action.REBUILD_CHUNK,
                     whenTurnedOn = CacheVersion.Action.REBUILD_CHUNK,
                     whenTurnedOff = CacheVersion.Action.CLEAN_EXPERIMENTAL_CACHES,
                     isEnabled = { IncrementalCompilation.isExperimental() })

fun dataContainerCacheVersion(dataRoot: File): CacheVersion =
        CacheVersion(ownVersion = DATA_CONTAINER_VERSION,
                     versionFile = File(dataRoot, DATA_CONTAINER_VERSION_FILE_NAME),
                     whenVersionChanged = CacheVersion.Action.REBUILD_ALL_KOTLIN,
                     whenTurnedOn = CacheVersion.Action.REBUILD_ALL_KOTLIN,
                     whenTurnedOff = CacheVersion.Action.CLEAN_DATA_CONTAINER,
                     isEnabled = { IncrementalCompilation.isExperimental() })

fun allCachesVersions(containerDataRoot: File, dataRoots: Iterable<File>): Iterable<CacheVersion> {
    val versions = arrayListOf<CacheVersion>()
    versions.add(dataContainerCacheVersion(containerDataRoot))

    for (dataRoot in dataRoots) {
        versions.add(normalCacheVersion(dataRoot))
        versions.add(experimentalCacheVersion(dataRoot))
    }

    return versions
}
