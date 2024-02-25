// WITH_STDLIB
// KT-66080
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// KT-66081
// IGNORE_BACKEND: WASM
import kotlin.test.*

fun box(): String {
    assertFailsWith(ArithmeticException::class, { 5 / 0 })
    assertFailsWith(ArithmeticException::class, { 5 % 0 })
    assertEquals(1, 5 / try { 0 / 0; 1 } catch (e: ArithmeticException) { 5 })
    assertEquals(Double.NaN, 0.0 / 0.0)

    return "OK"
}
