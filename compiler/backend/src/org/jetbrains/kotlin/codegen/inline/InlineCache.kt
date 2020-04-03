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

package org.jetbrains.kotlin.codegen.inline

import com.intellij.util.containers.SLRUMap
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.org.objectweb.asm.commons.Method

data class MethodId(val ownerInternalName: String, val method: Method)

class InlineCache {
    val classBytes: SLRUMap<ClassId, ByteArray> = SLRUMap(30, 20)
    val methodNodeById: SLRUMap<MethodId, SMAPAndMethodNode> = SLRUMap(60, 50)
}

inline fun <K, V : Any> SLRUMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    val value = get(key)
    return if (value == null) {
        val answer = defaultValue()
        put(key, answer)
        answer
    } else {
        value
    }
}
