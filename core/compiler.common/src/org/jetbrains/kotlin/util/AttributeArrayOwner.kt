/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import kotlin.reflect.KClass

/**
 * [AttributeArrayOwner] based on different implementations of [ArrayMap] and switches them
 *   depending on array map fullness
 * [AttributeArrayOwner] can be used in classes with many instances,
 *   like user data for Fir elements or attributes for cone types
 *
 * Note that you can remove attributes from [AttributeArrayOwner] despite
 *   from components in [ComponentArrayOwner]
 */
abstract class AttributeArrayOwner<K : Any, T : Any> protected constructor(
    arrayMap: ArrayMap<T>,
) : AbstractArrayMapOwner<K, T>() {
    final override var arrayMap: ArrayMap<T> = arrayMap
        private set

    @Suppress("UNCHECKED_CAST")
    constructor() : this(EmptyArrayMap as ArrayMap<T>)

    final override fun registerComponent(keyQualifiedName: String, value: T) {
        val id = typeRegistry.getId(keyQualifiedName)
        when (arrayMap.size) {
            0 -> {
                val map = arrayMap
                if (map !is EmptyArrayMap) {
                    throw IllegalStateException(buildDiagnosticMessage(map, expectedSize = 0, expectedImplementation = "EmptyArrayMap"))
                }
                arrayMap = OneElementArrayMap(value, id)
                return
            }

            1 -> {
                val mapSnapshot = arrayMap
                val map = try {
                    mapSnapshot as OneElementArrayMap<T>
                } catch (e: ClassCastException) {
                    throw IllegalStateException(
                        buildDiagnosticMessage(mapSnapshot, expectedSize = 1, expectedImplementation = "OneElementArrayMap"),
                        /*cause=*/e
                    )
                }
                if (map.index == id) {
                    arrayMap = OneElementArrayMap(value, id)
                    return
                } else {
                    arrayMap = ArrayMapImpl()
                    arrayMap[map.index] = map.value
                }
            }
        }

        arrayMap[id] = value
    }

    private fun buildDiagnosticMessage(map: ArrayMap<T>, expectedSize: Int, expectedImplementation: String): String {
        return buildString {
            appendLine("Race condition happened, the size of ArrayMap is $expectedSize but it isn't an `$expectedImplementation`")
            appendLine("Type: ${map::class.java}")
            val content = buildString {
                val services = typeRegistry.allValuesThreadUnsafeForRendering()
                appendLine("[")
                map.mapIndexed { index, value ->
                    val service = services.entries.firstOrNull { it.value == index }
                    appendLine("  $service[$index]: $value")
                }
                appendLine("]")
            }
            appendLine("Content: $content")
        }
    }

    protected fun removeComponent(tClass: KClass<out K>) {
        val id = typeRegistry.getId(tClass)
        if (arrayMap[id] == null) return
        @Suppress("UNCHECKED_CAST")
        when (arrayMap.size) {
            1 -> arrayMap = EmptyArrayMap as ArrayMap<T>
            else -> {
                val map = arrayMap as ArrayMapImpl<T>
                map.remove(id)
                if (map.size == 1) {
                    val (index, value) = map.entries().first()
                    arrayMap = OneElementArrayMap(value, index)
                }
            }
        }
    }
}
