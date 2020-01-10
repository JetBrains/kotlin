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

import org.jetbrains.annotations.TestOnly
import java.io.File
import java.io.IOException

open class BasicMapsOwner(val cachesDir: File) {
    private val maps = arrayListOf<BasicMap<*, *>>()

    companion object {
        val CACHE_EXTENSION = "tab"
    }

    protected val String.storageFile: File
        get() = File(cachesDir, this + "." + CACHE_EXTENSION)

    protected fun <K, V, M : BasicMap<K, V>> registerMap(map: M): M {
        maps.add(map)
        return map
    }

    open fun clean() {
        forEachMapSafe("clean", BasicMap<*, *>::clean)
    }

    open fun close() {
        forEachMapSafe("close", BasicMap<*, *>::close)
    }

    open fun flush(memoryCachesOnly: Boolean) {
        forEachMapSafe("flush") { it.flush(memoryCachesOnly) }
    }

    private fun forEachMapSafe(actionName: String, action: (BasicMap<*, *>) -> Unit) {
        val actionExceptions = LinkedHashMap<String, Exception>()
        maps.forEach {
            try {
                action(it)
            } catch (e: Exception) {
                actionExceptions[it.storageFile.name] = e
            }
        }
        if (actionExceptions.isNotEmpty()) {
            val desc = "Could not $actionName incremental caches in $cachesDir: ${actionExceptions.keys.joinToString(", ")}"
            val allIOExceptions = actionExceptions.all { it is IOException }
            val ex = if (allIOExceptions) IOException(desc) else Exception(desc)
            actionExceptions.forEach { (_, e) -> ex.addSuppressed(e) }
            throw ex
        }
    }

    @TestOnly
    fun dump(): String = maps.joinToString("\n\n") { it.dump() }
}