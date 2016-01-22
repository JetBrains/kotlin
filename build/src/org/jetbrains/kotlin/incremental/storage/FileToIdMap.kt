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

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.utils.keysToMap
import java.io.File

internal class FileToIdMap(file: File) : BasicMap<File, Int>(file, FileKeyDescriptor, IntExternalizer) {
    override fun dumpKey(key: File): String = key.toString()

    override fun dumpValue(value: Int): String = value.toString()

    operator fun get(file: File): Int? = storage[file]

    operator fun set(file: File, id: Int) {
        storage[file] = id
    }

    fun remove(file: File) {
        storage.remove(file)
    }

    fun toMap(): Map<File, Int> = storage.keys.keysToMap { storage[it]!! }
}
