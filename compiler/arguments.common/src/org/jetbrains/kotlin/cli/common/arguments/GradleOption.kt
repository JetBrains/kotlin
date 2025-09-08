/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

/**
 * @param value should be one of [DefaultValue] enum values
 * @param gradleInputType should be one of [GradleInputTypes] enum values
 */
class GradleOption(
    val value: DefaultValue,
    val gradleInputType: GradleInputTypes,
    val shouldGenerateDeprecatedKotlinOptions: Boolean = false,
    val gradleName: String = "",
)

enum class DefaultValue {
    BOOLEAN_FALSE_DEFAULT,
    BOOLEAN_TRUE_DEFAULT,
    BOOLEAN_NULL_DEFAULT,
    STRING_NULL_DEFAULT,
    EMPTY_STRING_LIST_DEFAULT,
    EMPTY_STRING_ARRAY_DEFAULT,
    LANGUAGE_VERSIONS,
    API_VERSIONS,
    JVM_TARGET_VERSIONS,
    JVM_DEFAULT_MODES,
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
