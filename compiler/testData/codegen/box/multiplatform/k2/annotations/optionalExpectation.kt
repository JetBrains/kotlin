// !LANGUAGE: +MultiPlatformProjects
// !OPT_IN: kotlin.ExperimentalMultiplatform
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, NATIVE
// WITH_STDLIB

// MODULE: common
// FILE: expected.kt

package a

@OptionalExpectation
expect annotation class A(val x: Int)

@OptionalExpectation
expect annotation class B(val s: String) {
    @OptionalExpectation
    annotation class C(val a: Boolean)
}

// MODULE: library()()(common)
// FILE: library.kt

package a

actual annotation class A(actual val x: Int)

// MODULE: main(library)
// FILE: main.kt

package usage

import a.A
import a.B

@A(42)
@B("OK")
@B.C(true)
fun box(): String {
    return "OK"
}
