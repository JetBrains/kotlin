// LANGUAGE: +MultiPlatformProjects, +FunctionalTypeWithExtensionAsSupertype
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^ IMPLEMENTING_FUNCTION_INTERFACE: Implementing function interface is prohibited in JavaScript
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

// MODULE: common
// FILE: common.kt

package test

expect class A: Int.() -> String {
    override fun invoke(p1: Int): String
}

// MODULE: platform()()(common)
// FILE: platform.kt

package test

actual class A: Int.() -> String {
    actual override fun invoke(p1: Int): String {
        return "OK"
    }
}

fun box(): String = A()(1)