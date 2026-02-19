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
import org.jetbrains.kotlin.codegen.state.CompiledCodeProvider
import org.jetbrains.org.objectweb.asm.commons.Method

data class MethodId(val ownerInternalName: String, val method: Method)

/**
 * A bytecode cache for function call inliner.
 * The cache is *not* concurrent-safe.
 *
 * @param provider A code provider to use if the class is not in the cache.
 */
class InlineCache(val provider: CompiledCodeProvider) : CompiledCodeProvider {
    private val classBytes: SLRUMap<String, ByteArray> = SLRUMap(30, 20)
    private val methodNodeById: SLRUMap<MethodId, SMAPAndMethodNode> = SLRUMap(60, 50)

    override fun getClassBytes(className: String): ByteArray? {
        return provider.getClassBytes(className)
            ?: classBytes[className]
    }

    /**
     * Returns cached or externally provided bytes for the class with the given [className].
     * If the class is not yet cached, bytes are computed by calling the [computer]. The result is then put in the cache.
     *
     * The method does not impose any specific caching behavior.
     * The [computer] may be called more than once for the same [className].
     */
    fun computeClassBytes(className: String, computer: () -> ByteArray): ByteArray {
        return provider.getClassBytes(className)
            ?: classBytes.getOrPut(className, computer)
    }

    /**
     * Returns the cached method node for the method with the given [methodId].
     * If the method information is not yet cached, it is computed by calling the [computer]. The result is then put in the cache.
     *
     * You might want to call [computeClassBytes] inside the passed [computer] to get the containing class bytecode.
     *
     * The method does not impose any specific caching behavior.
     * The [computer] may be called more than once for the same [methodId].
     */
    fun computeMethodBytes(methodId: MethodId, computer: () -> SMAPAndMethodNode): SMAPAndMethodNode {
        return methodNodeById.getOrPut(methodId, computer)
    }
}

private inline fun <K, V : Any> SLRUMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    synchronized(this) {
        val value = get(key)
        return if (value == null) {
            val answer = defaultValue()
            put(key, answer)
            answer
        } else {
            value
        }
    }
}
