/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.config

import java.util.HashMap

public class CompilerServices private(private val map: Map<Class<*>, Any>) {

    public fun <T> get(interfaceClass: Class<T>): T {
        return map.get(interfaceClass) as T
    }

    public class Builder {
        private val map = HashMap<Class<*>, Any>()

        public fun <T> register(interfaceClass: Class<T>, implementation: T): Builder {
            map.put(interfaceClass, implementation)
            return this
        }

        public fun build(): CompilerServices {
            return CompilerServices(map)
        }
    }

    public class object {
        public val empty: CompilerServices
            get() = Builder().build()
    }
}
