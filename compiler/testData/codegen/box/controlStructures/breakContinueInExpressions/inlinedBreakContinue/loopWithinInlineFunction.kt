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
    val visited = mutableListOf<Pair<Int, Int>>()

    for (i in 1..3) {
        (1..3).myForEach { j ->
            if (j == 3) {
                break
            }
            visited += i to j
        }
    }

    assertEquals(listOf(1 to 1, 1 to 2), visited)
    return "OK"
}