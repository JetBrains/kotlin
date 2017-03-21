//
// Example startup helper for Kotlin N compiler. Compile to JAR and add it to
// Kotlin N classpath - it will get loaded and executed automatically.
//
package org.jetbrains.kotlin.konan

import org.jetbrains.kotlin.backend.konan.KonanTarget

// Name it Helper0 so that it get loaded.
class Helper0Disabled(
        val konanHome: String, val host: KonanTarget, val target: KonanTarget) : Runnable {
    override fun run() {
        println("Running helper with $konanHome $host $target")
    }
}