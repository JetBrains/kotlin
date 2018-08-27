/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
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