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

package org.jetbrains.kotlin.cfg.outofbound

public object MapUtils {
    public fun mapToString<K, V>(
            map: Map<K,V>,
            keyToString: (K) -> String = { it.toString() },
            valueToString: (V) -> String = { it.toString() }
    ): String {
        val mapAsString = map.toList().fold("") { acc, keyValue ->
            "$acc${keyToString(keyValue.first)}:${valueToString(keyValue.second)},"
        }
        if(!mapAsString.isEmpty()) {
            return "{${mapAsString.take(mapAsString.length() - 1)}}"
        }
        return "{$mapAsString}"
    }
}