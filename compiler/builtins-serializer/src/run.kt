/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.utils.builtinsSerializer

import java.io.File

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    if (args.size < 2) {
        println(
"""Kotlin built-ins serializer

Usage: ... <destination dir> (<built-ins src dir>)+

Analyzes Kotlin sources found in the given source directories and serializes
found top-level declarations to <destination dir> (files such as
.kotlin_class_names, .kotlin_name_table, .kotlin_package, *.kotlin_class)"""
        )
        return
    }

    val destDir = File(args[0])

    val srcDirs = args.iterator().skip(1).map({ File(it) }).toList()
    assert(srcDirs all { it.exists() }) { "Some of the built-ins source directories don't exist: $srcDirs" }

    BuiltInsSerializer(System.out).serialize(destDir, srcDirs)
}
