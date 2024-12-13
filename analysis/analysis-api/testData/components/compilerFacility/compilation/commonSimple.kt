// LANGUAGE: +MultiPlatformProjects

// MODULE: main
// TARGET_PLATFORM: Common
// FILE: main.kt
package app

expect class Foo() {
    val text: String
}

object Shared {
    val text = "Hello, world!"
}

fun main() {
    println(Foo().text)
}

// MODULE: jvm()()(main)
// TARGET_PLATFORM: JVM
// FILE: jvm.kt
package app

actual class Foo {
    actual val text = Shared.text
}