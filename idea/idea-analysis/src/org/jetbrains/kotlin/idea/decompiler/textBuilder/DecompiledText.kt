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

package org.jetbrains.kotlin.idea.decompiler.textBuilder

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.utils.keysToMap

data class DecompiledText(val text: String, val index: DecompiledTextIndex)

interface DecompiledTextIndexer<out T: Any> {
    fun indexDescriptor(descriptor: DeclarationDescriptor): Collection<T>
}

// In-memory HashMap-based index of decompiled text
// allows navigation features
class DecompiledTextIndex(private val indexers: Collection<DecompiledTextIndexer<*>>) {
    private val indexerToMap: Map<DecompiledTextIndexer<*>, MutableMap<Any, TextRange>> = indexers.keysToMap { hashMapOf<Any, TextRange>() }

    fun addToIndex(descriptor: DeclarationDescriptor, textRange: TextRange) {
        indexers.forEach { mapper ->
            val correspondingMap = indexerToMap[mapper]!!
            mapper.indexDescriptor(descriptor).forEach { key ->
                correspondingMap[key] = textRange
            }
        }
    }

    fun <T: Any> getRange(mapper: DecompiledTextIndexer<T>, key: T): TextRange? {
        return indexerToMap[mapper]?.get(key)
    }

    companion object {
        val Empty = DecompiledTextIndex(listOf())
    }
}
