/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.arguments.*
import org.jetbrains.kotlin.arguments.dsl.compilerArguments
import org.jetbrains.kotlin.arguments.types.BooleanType
import java.io.File

val deprecatedCommonArgs by compilerArgumentsLevel("commonCompilerArguments") {
    compilerArgument {
        name = "another-test"
        description = "TBA".asReleaseDependent()

        valueType = BooleanType()
        valueDescription = "true|false".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
            stabilizedVersion = KotlinReleaseVersion.v1_4_0,
            deprecatedVersion = KotlinReleaseVersion.v1_9_20,
            removedVersion = KotlinReleaseVersion.v2_0_0,
        )
    }

    subLevel("jvmCompilerArguments") {
        compilerArgument {
            name = "old-option"
            description = "TBA".asReleaseDependent()

            valueType = BooleanType()
            valueDescription = "true|false".asReleaseDependent()

            lifecycle(
                introducedVersion = KotlinReleaseVersion.v1_4_0,
                deprecatedVersion = KotlinReleaseVersion.v1_9_20,
                removedVersion = KotlinReleaseVersion.v2_0_0,
            )
        }
    }
}

val kotlinCompilerArguments = compilerArguments {
    topLevel(Levels.commonToolArguments, mergeWith = setOf(actualCommonToolsArguments)) {
        subLevel(Levels.commonCompilerArguments, mergeWith = setOf(actualCommonCompilerArguments, deprecatedCommonArgs)) {
            subLevel(Levels.jvmCompilerArguments, mergeWith = setOf(actualJvmCompilerArguments)) {}
        }
    }
}

fun main() {
    val format = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    val jsonArguments = format.encodeToString(kotlinCompilerArguments)
    println("=== arguments in JSON ===")
    println(jsonArguments)
    println("=== end of JSON ===")
    val jsonFile = File("./compiler/arguments/arguments.json")
    jsonFile.writeText(jsonArguments)

    val decodedCompilerArguments = format.decodeFromString<CompilerArguments>(jsonArguments)
    println("Decoded arguments: $decodedCompilerArguments")
}
