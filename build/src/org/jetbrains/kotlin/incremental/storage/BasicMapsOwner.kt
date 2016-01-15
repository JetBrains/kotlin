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

open class BasicMapsOwner {
    private val maps = arrayListOf<BasicMap<*, *>>()

    companion object {
        val CACHE_EXTENSION = "tab"
    }

    protected fun <K, V, M : BasicMap<K, V>> registerMap(map: M): M {
        maps.add(map)
        return map
    }

    open fun clean() {
        maps.forEach { it.clean() }
    }

    open fun close() {
        maps.forEach { it.close() }
    }

    open fun flush(memoryCachesOnly: Boolean) {
        maps.forEach { it.flush(memoryCachesOnly) }
    }

    @TestOnly fun dump(): String = maps.map { it.dump() }.joinToString("\n\n")
}