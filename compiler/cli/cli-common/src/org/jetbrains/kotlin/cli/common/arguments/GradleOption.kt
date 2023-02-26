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
 * @param value should be one of [DefaultValue] enum values
 * @param gradleInputType should be one of [GradleInputTypes] enum values
 */
@Retention(AnnotationRetention.RUNTIME)
annotation class GradleOption(
    val value: DefaultValue,
    val gradleInputType: GradleInputTypes,
    val shouldGenerateDeprecatedKotlinOptions: Boolean = false
)

enum class DefaultValue {
    BOOLEAN_FALSE_DEFAULT,
    BOOLEAN_TRUE_DEFAULT,
    STRING_NULL_DEFAULT,
    EMPTY_STRING_LIST_DEFAULT,
    EMPTY_STRING_ARRAY_DEFAULT,
    LANGUAGE_VERSIONS,
    API_VERSIONS,
    JVM_TARGET_VERSIONS,
    JS_ECMA_VERSIONS,
    JS_MODULE_KINDS,
    JS_SOURCE_MAP_CONTENT_MODES,
    JS_MAIN,
    JS_SOURCE_MAP_NAMES_POLICY,
}

enum class GradleInputTypes(val gradleType: String) {
    INPUT("org.gradle.api.tasks.Input"),
    INTERNAL("org.gradle.api.tasks.Internal")
}
