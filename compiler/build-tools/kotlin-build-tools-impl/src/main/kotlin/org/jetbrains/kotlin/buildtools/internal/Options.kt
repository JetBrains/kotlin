/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

internal class Options(
    private val optionsName: String,
) : DeepCopyable<Options> {
    constructor(typeForName: KClass<*>) : this(typeForName.qualifiedName ?: typeForName.jvmName)

    private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

    @UseFromImplModuleRestricted
    operator fun <V> set(key: BaseOption<V>, value: Any?) {
        optionsMap[key.id] = value
    }

    @UseFromImplModuleRestricted
    @Suppress("UNCHECKED_CAST")
    operator fun <V> get(key: BaseOption<V>): V = get(key.id)

    operator fun <V> get(key: BaseOptionWithDefault<V>): V = get(key.id)

    operator fun <V> set(key: BaseOptionWithDefault<V>, value: Any?) {
        optionsMap[key.id] = value
    }

    operator fun set(key: String, value: Any?) {
        optionsMap[key] = value
    }

    operator fun <V> get(key: String): V {
        @Suppress("UNCHECKED_CAST")
        return when (key) {
            in optionsMap -> optionsMap[key] as V
            else -> error("$key was not set in $optionsName")
        }
    }

    override fun deepCopy(): Options {
        return Options(optionsName).also { newOptions ->
            newOptions.optionsMap.putAll(optionsMap.entries.map {
                it.key to when (val value = it.value) {
                    is DeepCopyable<*> -> value.deepCopy()
                    else -> value
                }
            })
        }
    }
}

@RequiresOptIn("Don't use from -impl package, as we're not allowed to access API classes for backward compatibility reasons.")
internal annotation class UseFromImplModuleRestricted

internal fun initializeOptions(klazz: KClass<*>, options: Options) {
    // Use Java reflection to avoid triggering Kotlin reflection hierarchy resolution,
    // which fails when running against an older API version that doesn't have all supertypes.
    var jClass: Class<*>? = klazz.java
    while (jClass != null && jClass != Any::class.java) {
        val companionClass = jClass.declaredClasses.firstOrNull { it.simpleName == "Companion" }
        if (companionClass != null) {
            val companionField = try { jClass.getDeclaredField("Companion") } catch (_: NoSuchFieldException) { null }
            val companionInstance = companionField?.also { it.isAccessible = true }?.get(null)
            if (companionInstance != null) {
                companionClass.declaredMethods.filter { method ->
                    BaseOptionWithDefault::class.java.isAssignableFrom(method.returnType)
                }.forEach { method ->
                    @Suppress("UNCHECKED_CAST") val option = method.invoke(companionInstance) as BaseOptionWithDefault<*>
                    options[option.id] = option.defaultValue
                }
            }
        }
        jClass = jClass.superclass
    }
}

@TestOnly
@Suppress("unused")
internal fun overrideVersionForOptionsCheck(version: String) {
    versionForOptionsCheck = version
}

private var versionForOptionsCheck: String = KotlinCompilerVersion.VERSION

internal fun checkOptionIsAvailableForVersion(key: BaseOption<*>) {
    val getAvailableSinceVersionMethod = key::class.java.methods.find { it.name == "getAvailableSinceVersion" } ?: return
    val availableSinceVersion = getAvailableSinceVersionMethod.invoke(key) as KotlinReleaseVersion
    if (availableSinceVersion.toKotlinToolingVersion() > KotlinToolingVersion(versionForOptionsCheck).clearClassifier()) {
        throw IllegalStateException("${key.id} is available only since $availableSinceVersion")
    }
}

internal fun KotlinReleaseVersion.toKotlinToolingVersion(): KotlinToolingVersion {
    return KotlinToolingVersion(major, minor, patch, null)
}

private fun KotlinToolingVersion.clearClassifier(): KotlinToolingVersion {
    return KotlinToolingVersion(major, minor, patch, null)
}
