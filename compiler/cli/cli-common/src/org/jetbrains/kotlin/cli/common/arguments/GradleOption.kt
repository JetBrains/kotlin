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

import kotlin.reflect.KClass
import kotlin.reflect.KVisibility

/**
 * @param value should be one of [DefaultValue] constants
 * @param gradleInputType should be one of [GradleInputTypes] constants
 */
@Retention(AnnotationRetention.RUNTIME)
annotation class GradleOption(
    val value: Int,
    val gradleInputType: String,
    val shouldGenerateDeprecatedKotlinOptions: Boolean = false
)

// Enum class here is not possible due to bug in K2 compiler:
// https://youtrack.jetbrains.com/issue/KT-54079
object DefaultValue {
    const val BOOLEAN_FALSE_DEFAULT = 0
    const val BOOLEAN_TRUE_DEFAULT = 1
    const val STRING_NULL_DEFAULT = 2
    const val EMPTY_STRING_LIST_DEFAULT = 3
    const val LANGUAGE_VERSIONS = 4
    const val API_VERSIONS = 5
    const val JVM_TARGET_VERSIONS = 6
    const val JS_ECMA_VERSIONS = 7
    const val JS_MODULE_KINDS = 8
    const val JS_SOURCE_MAP_CONTENT_MODES = 9
    const val JS_MAIN = 10
    const val JS_SOURCE_MAP_NAMES_POLICY = 11
}

// Enum class here is not possible due to bug in K2 compiler:
// https://youtrack.jetbrains.com/issue/KT-54079
object GradleInputTypes {
    const val INPUT = "org.gradle.api.tasks.Input"
    const val INTERNAL = "org.gradle.api.tasks.Internal"
}
