// LANGUAGE: +BreakContinueInInlineLambdas
// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: WASM
// WITH_STDLIB

import kotlin.test.assertEquals

inline fun <T> Iterable<T>.myForEach(action: (T) -> Unit): Unit {
    for (element in this) action(element)
}


fun box(): String {
    while (true) {
        "".let { it.run { break } }
    }

    return "OK"
}