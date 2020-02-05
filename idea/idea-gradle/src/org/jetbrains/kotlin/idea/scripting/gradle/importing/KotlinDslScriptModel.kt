/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

data class KotlinDslScriptModel(
    val file: String,
    val inputsTimeStamp: Long,
    val classPath: List<String>,
    val sourcePath: List<String>,
    val imports: List<String>,
    val messages: List<Message>
) {
    data class Message(
        val severity: Severity,
        val text: String,
        val position: Position? = null
    )

    data class Position(val line: Int, val column: Int)

    enum class Severity { WARNING, ERROR }
}