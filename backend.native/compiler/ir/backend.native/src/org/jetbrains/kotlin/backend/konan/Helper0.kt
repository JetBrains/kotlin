//
// Example startup helper for Kotlin N compiler. Compile to JAR and add it to
// Kotlin N classpath - it will get loaded and executed automatically.
//
package org.jetbrains.kotlin.konan

import java.util.*

// Name it CompilerHelper0 so that it get loaded.
class Helper0Disabled(val dependenciesDir: String,
                      val properties: Properties,
                      val dependencies: List<String>) : Runnable {
    override fun run() {
        println("Running helper for $dependenciesDir, $properties, $dependencies")
    }
}