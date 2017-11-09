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
@file:JvmName("PreprocessorCLI")
package org.jetbrains.kotlin.preprocessor

import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    if (args.size != 3) {
        println("Usage: <path to sources> <output path> <profile>")
        System.exit(1)
    }

    val sourcePath = File(args[0])
    val targetPath = File(args[1])

    val profile = createProfile(args[2], targetPath)

    println("Preprocessing sources in $sourcePath to $targetPath with profile ${profile.name}")
    Preprocessor().processSources(sourcePath, profile)
}
