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

package org.jetbrains.kotlin.preprocessor

import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    require(args.size() == 2, "Please specify path to sources and output path for all platforms")

    val sourcePath = File(args[0])
    val targetPath = File(args[1])

    val profiles = listOf(6, 7, 8).map { createJvmProfile(targetPath, version = it) } + createJsProfile(targetPath)

    val pool = Executors.newCachedThreadPool()

    profiles.forEach { profile -> pool.submit { Preprocessor().processSources(sourcePath, profile) } }

    pool.shutdown()
    pool.awaitTermination(1, TimeUnit.MINUTES)
}
