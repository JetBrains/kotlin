// NO_CHECK_LAMBDA_INLINING

// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM
// LANGUAGE: -IrIntraModuleInlinerBeforeKlibSerialization -IrCrossModuleInlinerBeforeKlibSerialization
// ^^^ KT-78537 Expected: foo, Actual: <anonymous>$foo

// FILE: 1.kt
package test

inline fun <T> myRun(block: () -> T) = block()

// FILE: 2.kt
import test.*

fun box(): String {
    val name = myRun {
        fun foo() = "fail 1"
        val fooRef = ::foo
        fooRef.name
    }
    return if (name == "foo") "OK" else name
}
