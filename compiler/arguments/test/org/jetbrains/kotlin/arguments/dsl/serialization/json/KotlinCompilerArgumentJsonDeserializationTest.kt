/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.serialization.json

import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArguments
import org.junit.jupiter.api.Test
import java.io.File

class KotlinCompilerArgumentJsonDeserializationTest {

    @Test
    fun jsonIsDeserializable() {
        val jsonFile = File(this::class.java.classLoader.getResource(".")!!.path) // build/classes/kotlin/test
            .parentFile.parentFile.parentFile.parentFile
            .resolve("resources")
            .resolve("kotlin-compiler-arguments.json")
        val json = jsonFile.readText()

        Json.decodeFromString<KotlinCompilerArguments>(json)
    }
}