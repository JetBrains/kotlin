/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmCompilationOperationImpl
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

internal class Options(private val optionsName: String) {
    constructor(typeForName: KClass<*>) : this(typeForName::class.qualifiedName ?: typeForName::class.jvmName)

    private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

    @UseFromImplModuleRestricted
    operator fun <V> set(key: BaseOption<V>, value: Any?) {
        optionsMap[key.id] = value
    }

    @UseFromImplModuleRestricted
    @Suppress("UNCHECKED_CAST")
    operator fun <V> get(key: BaseOption<V>): V = get(key.id)

    operator fun <V> get(key: BaseOptionWithDefault<V>): V = if (key.id in optionsMap) {
        get(key.id)
    } else {
        key.defaultValue
    }

    operator fun set(key: String, value: Any?) {
        optionsMap[key] = value
    }

    operator fun <V> get(key: String): V {
        @Suppress("UNCHECKED_CAST") return if (key !in optionsMap) {
            error("$key was not set in $optionsName")
        } else optionsMap[key] as V
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Options

        if (optionsName != other.optionsName) return false
        if (optionsMap != other.optionsMap) return false

        return true
    }

    override fun hashCode(): Int {
        var result = optionsName.hashCode()
        result = 31 * result + optionsMap.entries.sumOf {
            it.key.hashCode() * 31 + modifiedHashCode(it.value)
        }
        return result
    }

    fun modifiedHashCode(v: Any?): Int {
        return when (v) {
            is Enum<*> -> v.name.hashCode()
            is Path -> v.absolutePathString().removePrefix(JvmCompilationOperationImpl.projectRootPath).hashCode()
            is String -> v.replace(JvmCompilationOperationImpl.projectRootPath, "").hashCode()
            is Iterable<*> -> v.sumOf { modifiedHashCode(it) }
            is Map<*, *> -> v.entries.sumOf { modifiedHashCode(it.key) * 31 + modifiedHashCode(it.value) }
            else -> v.hashCode()
        }
    }


//    override fun equals(other: Any?): Boolean {
//        return optionsMap == other?.let { (it as? Options).optionsMap }
//    }
//
//    override fun hashCode(): Int {
//        return optionsMap.entries.sumOf { it.key.hashCode() + when(it.value) {
//                is Enum<*> -> (it.value as Enum<*>).ordinal.hashCode()
//                else -> it.value.hashCode()
//            }
//        }
//    }
}

@RequiresOptIn("Don't use from -impl package, as we're not allowed to access API classes for backward compatibility reasons.")
internal annotation class UseFromImplModuleRestricted