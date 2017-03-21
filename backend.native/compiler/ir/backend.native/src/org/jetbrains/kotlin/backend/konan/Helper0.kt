//
// Example startup helper for Kotlin N compiler. Compile to JAR and add it to
// Kotlin N classpath - it will get loaded and executed automatically.
//
package org.jetbrains.kotlin.konan

import org.jetbrains.kotlin.backend.konan.Distribution
import org.jetbrains.kotlin.backend.konan.TargetManager

// Name it CompilerHelper0 so that it get loaded.
class CompilerHelper0Disabled(val distribution: Distribution) : Runnable {
    override fun run() {
        println("Running helper with ${distribution.konanHome} ${distribution.target} ${TargetManager.host}")
    }
}

//
// Example startup helper for Kotlin N stub generator. Compile to JAR and add it to
// Kotlin N classpath - it will get loaded and executed automatically.
//

// Name it InteropHelper0 so that it get loaded.
class InteropHelper0Disabled(val dependenciesRoot: String, val dependencies: List<String>) : Runnable {
    override fun run() {
        println("Running helper with $dependenciesRoot $dependencies")
    }
}