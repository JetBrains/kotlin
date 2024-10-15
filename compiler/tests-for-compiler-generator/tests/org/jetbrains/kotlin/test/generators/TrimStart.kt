/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.generators

import org.jetbrains.kotlin.test.runners.TestTiers
import java.io.File

fun main() {
    val diagnosticsDir = File("/Users/Nikolay.Lunyak/Documents/Projects/kotlin-worktrees/kotlin-uber-test-runner/compiler/testData/diagnostics")

    val dirs = listOf(
        diagnosticsDir.resolve("tests"),
        diagnosticsDir.resolve("testsWithStdLib"),
        diagnosticsDir.resolve("testsWithJvmBackend"),
    )

    for (dir in dirs) {
        dir.walkTopDown().forEach { file ->
            if (file.isDirectory) {
                return@forEach
            }

            val allText = file.readText().replace("\r\n", "\n")

            if (file.wideExtension == "kt" && allText.split("\n").none { it.startsWith("// RUN_PIPELINE_TILL:") }) {
                file.writeText(file.readText().trimStart())

                val firFile = file.parentFile.resolve(file.narrowName + ".fir.kt")

                if (firFile.exists()) {
                    firFile.writeText(firFile.readText().trimStart())
                }

                val llFile = file.parentFile.resolve(file.narrowName + ".ll.kt")

                if (llFile.exists()) {
                    llFile.writeText(llFile.readText().trimStart())
                }
            }
        }
    }
}

private val File.wideExtension get() = name.split(".", limit = 2).getOrNull(1) ?: ""
private val File.narrowName get() = name.split(".", limit = 2).first()
