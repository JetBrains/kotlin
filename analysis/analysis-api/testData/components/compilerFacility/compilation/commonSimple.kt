// LANGUAGE: +MultiPlatformProjects, +ExpectRefinement
//                                   ^^^ Circumvent misconfiguration in the test infra KT-75281

// MODULE: main
// TARGET_PLATFORM: Common
// FILE: main.kt
package app

@OptIn(ExperimentalMultiplatform::class)
@kotlin.experimental.ExpectRefinement // Circumvent misconfiguration in the test infra KT-75281
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