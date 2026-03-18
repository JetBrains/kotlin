/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import com.intellij.openapi.util.Key
import java.util.*

class CompilerConfiguration {
    companion object {
        private fun <T> T.unmodifiable(): T {
            @Suppress("UNCHECKED_CAST")
            return when (this) {
                is List<*> -> Collections.unmodifiableList(this)
                is Map<*, *> -> Collections.unmodifiableMap(this)
                is Set<*> -> Collections.unmodifiableSet(this)
                is Collection<*> -> Collections.unmodifiableCollection(this)
                else -> this
            } as T
        }
    }

    @Internals("Consider using `CompilerConfiguration.create()` from :cli-base module instead")
    constructor()

    private val map: MutableMap<Key<*>, Any> = LinkedHashMap()
    var isReadOnly = false

    operator fun <T : Any> get(key: CompilerConfigurationKey<T>): T? {
        return getValue(key)?.unmodifiable()
    }

    operator fun <T : Any> get(key: CompilerConfigurationKey<T>, defaultValue: T): T {
        return getValue(key) ?: defaultValue
    }

    fun <T : Any> getOrDefault(key: CompilerConfigurationKey<T>, defaultValue: () -> T): T {
        return getValue(key) ?: defaultValue()
    }

    fun <T : Any> getNotNull(key: CompilerConfigurationKey<T>): T {
        return getValue(key) ?: error("No value for configuration key: $key")
    }

    fun getBoolean(key: CompilerConfigurationKey<Boolean>): Boolean {
        return get(key, defaultValue = false)
    }

    fun <T> getList(key: CompilerConfigurationKey<List<T>>): List<T> {
        return get(key, defaultValue = emptyList())
    }

    fun <K, V> getMap(key: CompilerConfigurationKey<Map<K, V>>): Map<K, V> {
        return get(key, defaultValue = emptyMap())
    }

    fun <T : Any> put(key: CompilerConfigurationKey<T>, value: T) {
        checkReadOnly()
        map[key.ideaKey] = value
    }

    fun <T : Any> putIfAbsent(key: CompilerConfigurationKey<T>, value: T): T {
        getValue(key)?.let { return it }
        checkReadOnly()
        put(key, value)
        return value
    }

    fun <T : Any> putIfNotNull(key: CompilerConfigurationKey<T>, value: T?) {
        if (value != null) {
            put(key, value)
        }
    }

    fun <T> add(key: CompilerConfigurationKey<List<T>>, value: T) {
        checkReadOnly()

        @Suppress("UNCHECKED_CAST")
        val list = map.getOrPut(key.ideaKey) { mutableListOf<T>() } as MutableList<T>
        list += value
    }

    fun <K, V> put(configurationKey: CompilerConfigurationKey<Map<K, V>>, key: K, value: V) {
        checkReadOnly()

        @Suppress("UNCHECKED_CAST")
        val map = map.getOrPut(configurationKey.ideaKey) { mutableMapOf<K, V>() } as MutableMap<K, V>
        map[key] = value
    }

    fun <T : Any> addAll(key: CompilerConfigurationKey<List<T>>, values: Collection<T>?) {
        if (values != null) {
            addAll(key, getList(key).size, values)
        }
    }

    fun <T : Any> addAll(key: CompilerConfigurationKey<List<T>>, index: Int, values: Collection<T>) {
        checkReadOnly()

        @Suppress("UNCHECKED_CAST")
        val list = map.getOrPut(key.ideaKey) { mutableListOf<T>() } as MutableList<T>
        list.addAll(index, values)
    }

    fun copy(): CompilerConfiguration {
        @OptIn(Internals::class)
        return CompilerConfiguration().also { it.map.putAll(map) }
    }

    private fun <T : Any> getValue(key: CompilerConfigurationKey<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return map[key.ideaKey] as T?
    }

    override fun toString(): String {
        return buildString {
            for ((key, value) in map) {
                append(key).append(":")
                when (value) {
                    is Collection<*> -> {
                        appendLine()
                        for (v in value) {
                            append("  ").appendLine(v)
                        }
                    }
                    is Map<*, *> -> {
                        appendLine()
                        for ((k, v) in value) {
                            append("  ").append(k).append("=").appendLine(v)
                        }
                    }
                    else -> append(" ").appendLine(value)
                }
            }
        }.trim()
    }

    private fun checkReadOnly() {
        check(!isReadOnly) { "CompilerConfiguration is read-only" }
    }

    @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
    annotation class Internals(val message: String)
}
