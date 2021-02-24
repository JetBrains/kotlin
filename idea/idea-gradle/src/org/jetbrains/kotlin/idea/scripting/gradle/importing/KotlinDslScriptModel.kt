/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import org.jetbrains.kotlin.idea.scripting.gradle.GradleKotlinScriptConfigurationInputs

data class KotlinDslScriptModel(
    val file: String,
    val inputs: GradleKotlinScriptConfigurationInputs,
    val classPath: List<String>,
    val sourcePath: List<String>,
    val imports: List<String>,
    val messages: List<Message>
) {
    data class Message(
        val severity: Severity,
        val text: String,
        val details: String = "",
        val position: Position?
    )

    data class Position(val line: Int, val column: Int)

    enum class Severity { WARNING, ERROR }
}

fun parsePositionFromException(e: String): Pair<String, KotlinDslScriptModel.Position>? {
    //org.gradle.internal.exceptions.LocationAwareException: Build file '...\build.gradle.kts' line: 21
    if (e.startsWith("org.gradle.internal.exceptions.LocationAwareException:")) {
        val message = e.substringBefore(System.lineSeparator())
        val file = message.substringAfter("Build file '").substringBefore("'")
        val line = message.substringAfter("line: ").toIntOrNull()
        if (file.isNotBlank() && line != null) {
            return file to KotlinDslScriptModel.Position(line, 0)
        }
    }
    return null
}