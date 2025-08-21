/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.arguments.description.kotlinCompilerArguments
import java.io.File

fun main(args: Array<String>) {
    val destinationFile = File(args.first())

    val format = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    val jsonArguments = format.encodeToString(kotlinCompilerArguments)

    destinationFile.writeText(jsonArguments)
}