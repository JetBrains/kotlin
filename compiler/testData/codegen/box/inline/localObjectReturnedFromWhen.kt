// Implementing a function interface is prohibited in JavaScript
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.
// WITH_STDLIB

import kotlin.test.*

fun foo() {
    123?.let {
        object : () -> Unit {
            override fun invoke() = Unit
        }
    }
}

fun box(): String {
    foo()
    return "OK"
}
