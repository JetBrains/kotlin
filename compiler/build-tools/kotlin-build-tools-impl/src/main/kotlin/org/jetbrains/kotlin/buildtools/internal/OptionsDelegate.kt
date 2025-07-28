/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmName


internal class OptionsDelegate() {
    private lateinit var options: Options
    operator fun getValue(thisRef: Any, property: KProperty<*>): Options {
        if (!::options.isInitialized) {
            options = Options(thisRef::class.qualifiedName ?: thisRef::class.jvmName)
        }
        return options
    }
}

internal class Options(private val optionsName: String) {
    private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

    @UseFromImplModuleRestricted
    operator fun <V> set(key: BaseOption<V>, value: Any?) {
        optionsMap[key.id] = value
    }

    @UseFromImplModuleRestricted
    @Suppress("UNCHECKED_CAST")
    operator fun <V> get(key: BaseOption<V>): V = get(key.id)

    operator fun set(key: String, value: Any?) {
        optionsMap[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <V> get(key: String): V {
        return if (key !in optionsMap) {
            error("$key was not set in $optionsName")
        } else optionsMap[key] as V
    }
}

@RequiresOptIn("Don't use from -impl package, as we're not allowed to access API classes for backward compatibility reasons.")
internal annotation class UseFromImplModuleRestricted