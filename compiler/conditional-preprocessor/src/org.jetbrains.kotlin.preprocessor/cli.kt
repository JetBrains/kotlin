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
    require(args.size() == 1, "Please specify path to sources")

    val sourcePath = File(args.first())

    val targetPath = File("libraries/stdlib/target")


    val profiles = listOf(6, 7, 8).map { Preprocessor(createJvmProfile(targetPath, version = it)) }

    val pool = Executors.newCachedThreadPool()

    profiles.forEach { pool.submit { it.processSources(sourcePath) } }

    pool.shutdown()
    pool.awaitTermination(1, TimeUnit.MINUTES)
}
