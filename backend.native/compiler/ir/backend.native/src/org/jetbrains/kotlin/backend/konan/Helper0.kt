/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

//
// Example startup helper for Kotlin/Native compiler. Compile to JAR and add it to
// Kotlin/Native classpath - it will get loaded and executed automatically.
//
package org.jetbrains.kotlin.konan.util

import java.util.*

// Name it Helper0 so that it get loaded.
class Helper0Disabled(val dependenciesDir: String,
                      val properties: Properties,
                      val dependencies: List<String>) : Runnable {
    override fun run() {
        println("Running helper for $dependenciesDir, $properties, $dependencies")
    }
}