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

import kotlin.reflect.KProperty1
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
