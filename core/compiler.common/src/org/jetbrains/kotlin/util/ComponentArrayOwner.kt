/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import kotlin.reflect.KClass

/**
 * [ComponentArrayOwner] based on [ArrayMap] with flexible size and should be used for
 *   storing services in entities with limited number of instances, like FirSession
 */
abstract class ComponentArrayOwner<K : Any, V : Any> : AbstractArrayMapOwner<K, V>() {
    final override val arrayMap: ArrayMap<V> =
        ArrayMapImpl()

    final override fun registerComponent(keyQualifiedName: String, value: V) {
        val id = typeRegistry.getId(keyQualifiedName)
        try {
            arrayMap[id] = value
        } catch (e: Exception) {
            throw RuntimeException(createDiagnosticMessage(id, keyQualifiedName), e)
        }
    }

    protected operator fun get(key: KClass<out K>): V {
        getOrNull(key)?.let { return it }
        val id = typeRegistry.getId(key)
        error("No '$key'($id) component in array: $this")
    }

    protected fun getOrNull(key: KClass<out K>): V? {
        val id = typeRegistry.getId(key)
        return arrayMap[id]
    }

    private fun createDiagnosticMessage(id: Int, keyQualifiedName: String): String = buildString {
        appendLine("Error occurred during registration of component in array")
        appendLine("Currently registered")
        appendLine("  $id: $keyQualifiedName")
        appendLine("Registrar:")
        for ((kClass, x) in typeRegistry.allValuesThreadUnsafeForRendering()) {
            appendLine("  $x: $kClass")
        }
        appendLine("Array map:")
        for (i in 0 until arrayMap.size) {
            var element: Any? = arrayMap[i]
            if (element != null) {
                element = element::class
            }
            appendLine("  $i: $element")
        }
    }
}
