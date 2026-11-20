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

package org.jetbrains.kotlin.cli.common.arguments

import com.intellij.util.text.VersionComparatorUtil
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * An argument which should be passed to Kotlin compiler to enable [this] compiler option
 */
val KProperty1<out CommonToolArguments, *>.argumentAnnotation: Argument
    get() {
        val javaField = javaField ?: error("Java field should be present for $this")
        return javaField.getAnnotation(Argument::class.java)
    }

/**
 * An argument which should be passed to Kotlin compiler to enable [this] compiler option
 */
val KProperty1<out CommonToolArguments, *>.cliArgument: String
    get() = argumentAnnotation.value

/**
 * Returns a string of the form "argument=value" where "argument" is the [Argument.value] of this compiler argument.
 */
fun KProperty1<out CommonToolArguments, *>.cliArgument(value: String): String {
    return "$cliArgument=$value"
}

fun K2NativeCompilerArguments.isNativeSecondStage(): Boolean = produce != "library"

/**
 * It's a helper function to parse version strings strictly from a `KotlinReleaseVersion.releaseName` value.
 * Since those values are always correct version strings, the function should never fail.
 */
internal fun parseKotlinVersion(kotlinReleaseVersion: String): KotlinVersion {
    val components = kotlinReleaseVersion.split('.')
    return KotlinVersion(
        major = components[0].toInt(),
        minor = components[1].toInt(),
        patch = components[2].toInt(),
    )
}

// The following functions are binary compatibility shims for prebuilt Kotlin JPS plugins (e.g. the pinned
// `kotlin-jps-plugin-classpath` in IntelliJ), which still call them at their old location. When such a JPS plugin runs
// against newer compiler jars, the old functions must still exist. The functions were moved to `build-common` in
// KT-89577. The shims can be removed once the pinned JPS plugin versions are past this move.

@Deprecated(
    "Moved to `org.jetbrains.kotlin.compilerRunner`. Kept only for binary compatibility.",
    level = DeprecationLevel.HIDDEN,
)
fun CommonCompilerArguments.setApiVersionToLanguageVersionIfNeeded() {
    if (languageVersion != null && VersionComparatorUtil.compare(languageVersion, apiVersion) < 0) {
        apiVersion = languageVersion
    }
}

@Deprecated(
    "Moved to `org.jetbrains.kotlin.compilerRunner`. Kept only for binary compatibility.",
    level = DeprecationLevel.HIDDEN,
)
fun <From : Any, To : From> mergeBeans(from: From, to: To): To {
    if (from == to) return to

    val toMemberProperties = to::class.memberProperties.associateBy { it.name }

    @Suppress("UNCHECKED_CAST")
    for (fromProperty in collectPropertiesForBinaryCompatibility(from::class as KClass<From>, inheritedOnly = false)) {
        @Suppress("UNCHECKED_CAST")
        val toProperty = toMemberProperties[fromProperty.name] as? KMutableProperty1<To, Any?>
            ?: continue
        toProperty.set(to, fromProperty.get(from))
    }
    return to
}

@Deprecated(
    "Moved to `org.jetbrains.kotlin.arguments`. Kept only for binary compatibility.",
    level = DeprecationLevel.HIDDEN,
)
fun <T : Any> collectProperties(kClass: KClass<T>, inheritedOnly: Boolean): List<KProperty1<T, Any?>> =
    collectPropertiesForBinaryCompatibility(kClass, inheritedOnly)

private fun <T : Any> collectPropertiesForBinaryCompatibility(kClass: KClass<T>, inheritedOnly: Boolean): List<KProperty1<T, Any?>> {
    val properties = ArrayList(kClass.memberProperties)
    if (inheritedOnly) {
        properties.removeAll(kClass.declaredMemberProperties)
    }
    return properties.filter { property ->
        property.visibility == KVisibility.PUBLIC
                && (property.javaField?.modifiers?.let { Modifier.isTransient(it) } != true)
                && (!property.isAbstract)
    }
}
