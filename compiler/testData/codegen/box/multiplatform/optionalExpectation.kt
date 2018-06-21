// !LANGUAGE: +MultiPlatformProjects
// !USE_EXPERIMENTAL: kotlin.ExperimentalMultiplatform
// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME
// MODULE: library
// FILE: expected.kt

package a

@OptionalExpectation
expect annotation class A(val x: Int)

@OptionalExpectation
expect annotation class B(val s: String)

// FILE: actual.kt

package a

actual annotation class A(actual val x: Int)

// MODULE: main(library)
// FILE: main.kt

package usage

import a.A
import a.B

@A(42)
@B("OK")
fun box(): String {
    return "OK"
}
