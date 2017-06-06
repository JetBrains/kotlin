/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.utils.DescriptionAware

class ProtocolsBackend(val backendType: BackendType, val cacheType: CacheType, val cacheSize: Int) {

    enum class BackendType(override val description: String) : DescriptionAware {
        REFLECTION("reflection"),
        INDY("indy");

        companion object {
            @JvmField
            val DEFAULT = INDY

            @JvmStatic
            fun fromString(backendType: String) = values().find { it.description == backendType }
        }
    }

    enum class CacheType(override val description: String) : DescriptionAware {
        ARRAY("array"),
        LRU("lru"),
        SYNCHRONIZED_LIST("sync_list"),
        RW_LIST("rw_list");

        companion object {
            @JvmField
            val DEFAULT = ARRAY

            @JvmStatic
            fun fromString(cacheType: String) = values().find { it.description == cacheType }
        }

    }

    companion object {
        @JvmField
        val DEFAULT_SIZE = 20

        @JvmField
        val DEFAULT = ProtocolsBackend(BackendType.INDY, CacheType.ARRAY, DEFAULT_SIZE)
    }
}
