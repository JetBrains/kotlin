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

package org.jetbrains.kotlin.analyzer

import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.SLRUMap
import org.jetbrains.kotlin.storage.StorageManager

class WeakReferenceSLRUCache<in K : Any, V : Any>(private val storageManager: StorageManager) {
    private val weakValueMap = ContainerUtil.createConcurrentWeakValueMap<K, V>()
    // Store most recently used values in SLRU cache to make them available even if no other references to them exist
    private val recentlyUsed = SLRUMap<V, Any>(20, 20)

    fun prepareValueComputation(module: K, computation: () -> V): () -> V = {
        getOrCompute(module, computation)
    }

    private fun getOrCompute(module: K, computation: () -> V): V {
        getFromWeakValueMap(module)?.let {
            return it
        }

        return storageManager.compute {
            getFromWeakValueMap(module)?.let {
                return@compute it
            }

            val result = computation()
            weakValueMap[module] = result

            recentlyUsed.put(result, STUB_OBJECT)

            result
        }
    }

    private fun getFromWeakValueMap(module: K) =
            weakValueMap[module]?.let {
                recordAccess(it)
                it
            }

    private fun recordAccess(resolver: V) {
        storageManager.compute {
            recentlyUsed.get(resolver)
        }
    }
}

private val STUB_OBJECT = Any()
