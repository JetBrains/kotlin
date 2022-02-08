// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: expect/actual in the same module (ACTUAL_WITHOUT_EXPECT)
// !LANGUAGE: +MultiPlatformProjects
// !OPT_IN: kotlin.ExperimentalMultiplatform
// IGNORE_BACKEND: NATIVE
// WITH_STDLIB
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

@file:Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE") // TODO: support common sources in the test infrastructure

package usage

import a.A
import a.B

@A(42)
@B("OK")
fun box(): String {
    return "OK"
}
