// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: KOTLIN_TEST_LIB
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    assertTrue(Long.MAX_VALUE !in Int.MIN_VALUE..Int.MAX_VALUE)
    assertTrue(Int.MAX_VALUE in Long.MIN_VALUE..Long.MAX_VALUE)
    assertTrue(Double.MAX_VALUE !in Float.MIN_VALUE..Float.MAX_VALUE)
    assertTrue(Float.MIN_VALUE in 0.0..1.0)
    assertTrue(2.0 !in 1.0f..0.0f)
    assertTrue(1L in 0..2)

    return "OK"
}
