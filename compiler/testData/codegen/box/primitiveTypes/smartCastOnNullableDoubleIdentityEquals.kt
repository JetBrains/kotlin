// WITH_STDLIB
// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6, NATIVE
// ISSUE: KT-82557, KT-82694, KT-82695
import kotlin.test.*

fun box(): String {
    val boxedZero: Double? = 0.0;
    assertFalse(boxedZero === -0.0)

    boxedZero!!
    assertTrue(boxedZero === -0.0)

    return "OK"
}