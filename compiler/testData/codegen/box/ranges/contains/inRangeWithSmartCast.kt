// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: KOTLIN_TEST_LIB
// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
import kotlin.test.assertTrue

fun check(x: Any?): Boolean {
    if (x is Int) {
        return x in 239..240
    }

    throw AssertionError()
}

fun check(x: Any?, l: Any?, r: Any?): Boolean {
    if (x is Int && l is Int && r is Int) {
       return x in l..r
    }

    throw AssertionError()
}


fun box(): String {
    assertTrue(check(239))
    assertTrue(check(239, 239, 240))
    assertTrue(!check(238))
    assertTrue(!check(238, 239, 240))
    return "OK"
}
