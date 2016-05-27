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

package org.jetbrains.kotlin.config

import java.util.*

class Services private constructor(private val map: Map<Class<*>, Any>) {
    companion object {
        @JvmField
        val EMPTY: Services = Builder().build()
    }

    fun <T> get(interfaceClass: Class<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return map[interfaceClass] as T?
    }

    class Builder {
        private val map = HashMap<Class<*>, Any>()

        fun <T : Any> register(interfaceClass: Class<T>, implementation: T): Builder {
            map.put(interfaceClass, implementation)
            return this
        }

        fun build(): Services {
            return Services(map)
        }
    }
}
