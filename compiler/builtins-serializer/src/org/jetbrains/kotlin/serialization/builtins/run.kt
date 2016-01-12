/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.serialization.builtins

import java.io.File

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    if (args.size < 2) {
        println(
"""Kotlin built-ins serializer

Usage: ... <destination dir> (<source dir>)+

Analyzes Kotlin sources found in the given source directories and serializes
found top-level declarations to <destination dir> (*.kotlin_builtins files)"""
        )
        return
    }

    val destDir = File(args[0])

    val srcDirs = args.drop(1).map { File(it) }
    assert(srcDirs.isNotEmpty()) { "At least one source directory should be specified" }

    val missing = srcDirs.filterNot { it.exists() }
    assert(missing.isEmpty()) { "These source directories are missing: $missing" }

    BuiltInsSerializer(dependOnOldBuiltIns = false).serialize(destDir, srcDirs, listOf()) { totalSize, totalFiles ->
        println("Total bytes written: $totalSize to $totalFiles files")
    }
}
