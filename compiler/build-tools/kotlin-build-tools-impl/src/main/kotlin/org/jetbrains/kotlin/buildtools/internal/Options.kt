/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

public interface HasFinalizableValues {
    public val isFinalized: Boolean
    public fun finalizeValues()
    public fun registerDependentFinalizableValues(values: HasFinalizableValues)
    public fun checkFinalized()
}

public class HasFinalizableValuesImpl() : HasFinalizableValues {
    private val dependentValues = mutableSetOf<HasFinalizableValues>()
    override var isFinalized: Boolean = false

    override fun finalizeValues() {
        isFinalized = true
        dependentValues.forEach { it.finalizeValues() }
    }

    override fun registerDependentFinalizableValues(values: HasFinalizableValues) {
        dependentValues.add(values)
    }

    override fun checkFinalized()  {
        if (isFinalized) {
            error("These configuration options have already been finalized and are locked from further updates.")
        }
    }
}


internal class Options private constructor(private val optionsName: String) : HasFinalizableValues by HasFinalizableValuesImpl() {
    private constructor(typeForName: KClass<*>) : this(typeForName::class.qualifiedName ?: typeForName::class.jvmName)

    private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

    @UseFromImplModuleRestricted
    operator fun <V> set(key: BaseOption<V>, value: Any?) {
        checkFinalized()
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
        checkFinalized()
        optionsMap[key] = value
    }

    operator fun <V> get(key: String): V {
        @Suppress("UNCHECKED_CAST")
        return if (key !in optionsMap) {
            error("$key was not set in $optionsName")
        } else optionsMap[key] as V
    }

    companion object {
        fun HasFinalizableValues.registerOptions(typeForName: KClass<*>): Options {
            return Options(typeForName).also { options ->
                registerDependentFinalizableValues(options)
            }
        }
    }
}

@RequiresOptIn("Don't use from -impl package, as we're not allowed to access API classes for backward compatibility reasons.")
internal annotation class UseFromImplModuleRestricted