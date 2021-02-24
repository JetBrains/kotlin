// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: KOTLIN_TEST_LIB
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    for (x in 1..10) {
        assertTrue(x in 1..10)
        assertTrue(x + 10 !in 1..10)
    }

    var x = 0
    assertTrue(0 !in 1..2)

    assertTrue(++x in 1..1)
    assertTrue(++x !in 1..1)

    assertTrue(sideEffect(x) in 2..3)
    return "OK"
}


var invocationCounter = 0
fun sideEffect(x: Int): Int {
    ++invocationCounter
    assertTrue(invocationCounter == 1)
    return x
}