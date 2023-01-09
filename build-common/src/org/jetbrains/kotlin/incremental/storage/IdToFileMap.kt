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

import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.ExternalIntegerKeyDescriptor
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import java.io.File

internal class IdToFileMap(
    file: File,
    icContext: IncrementalCompilationContext,
) : BasicMap<Int, String>(file, ExternalIntegerKeyDescriptor(), EnumeratorStringDescriptor.INSTANCE, icContext) {
    override fun dumpKey(key: Int): String = key.toString()

    override fun dumpValue(value: String): String = value

    operator fun get(id: Int): File? = storage[id]?.let { pathConverter.toFile(it) }

    operator fun contains(id: Int): Boolean = id in storage

    operator fun set(id: Int, file: File) {
        storage[id] = pathConverter.toPath(file)
    }

    fun remove(id: Int) {
        storage.remove(id)
    }
}
