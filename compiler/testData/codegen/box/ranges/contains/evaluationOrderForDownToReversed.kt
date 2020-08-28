// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

var order = StringBuilder()

inline fun expectOrder(at: String, expected: String, body: () -> Unit) {
    order = StringBuilder() // have to do that in order to run this test in JS
    body()
    if (order.toString() != expected) throw AssertionError("$at: expected: $expected, actual: $order")
}

fun low(i: Int): Int {
    order.append("L")
    return i
}

fun high(i: Int): Int {
    order.append("H")
    return i
}

fun x(i: Int): Int {
    order.append("X")
    return i
}

fun box(): String {
    expectOrder("x(0) in (high(3) downTo low(1)).reversed()", "HLX") { assertFalse(x(0) in (high(3) downTo low(1)).reversed()) }
    expectOrder("0 in (high(3) downTo low(1)).reversed()", "HL") { assertFalse(0 in (high(3) downTo low(1)).reversed()) }
    expectOrder("x(0) in high(3) downTo 1", "HX") { assertFalse(x(0) in high(3) downTo 1) }
    expectOrder("x(0) in 3 downTo low(1)", "LX") { assertFalse(x(0) in 3 downTo low(1)) }
    expectOrder("x(0) !in (high(3) downTo low(1)).reversed()", "HLX") { assertTrue(x(0) !in (high(3) downTo low(1)).reversed()) }
    expectOrder("0 !in (high(3) downTo low(1)).reversed()", "HL") { assertTrue(0 !in (high(3) downTo low(1)).reversed()) }
    expectOrder("x(0) !in high(3) downTo 1", "HX") { assertTrue(x(0) !in high(3) downTo 1) }
    expectOrder("x(0) !in 3 downTo low(1)", "LX") { assertTrue(x(0) !in 3 downTo low(1)) }

    expectOrder("x(4) in (high(3) downTo low(1)).reversed()", "HLX") { assertFalse(x(4) in (high(3) downTo low(1)).reversed()) }
    expectOrder("4 in (high(3) downTo low(1)).reversed()", "HL") { assertFalse(4 in (high(3) downTo low(1)).reversed()) }
    expectOrder("x(4) in high(3) downTo 1", "HX") { assertFalse(x(4) in high(3) downTo 1) }
    expectOrder("x(4) in 3 downTo low(1)", "LX") { assertFalse(x(4) in 3 downTo low(1)) }
    expectOrder("x(4) !in (high(3) downTo low(1)).reversed()", "HLX") { assertTrue(x(4) !in (high(3) downTo low(1)).reversed()) }
    expectOrder("4 !in (high(3) downTo low(1)).reversed()", "HL") { assertTrue(4 !in (high(3) downTo low(1)).reversed()) }
    expectOrder("x(4) !in high(3) downTo 1", "HX") { assertTrue(x(4) !in high(3) downTo 1) }
    expectOrder("x(4) !in 3 downTo low(1)", "LX") { assertTrue(x(4) !in 3 downTo low(1)) }

    expectOrder("x(2) in (high(3) downTo low(1)).reversed()", "HLX") { assertTrue(x(2) in (high(3) downTo low(1)).reversed()) }
    expectOrder("2 in (high(3) downTo low(1)).reversed()", "HL") { assertTrue(2 in (high(3) downTo low(1)).reversed()) }
    expectOrder("x(2) in high(3) downTo 1", "HX") { assertTrue(x(2) in high(3) downTo 1) }
    expectOrder("x(2) in 3 downTo low(1)", "LX") { assertTrue(x(2) in 3 downTo low(1)) }
    expectOrder("x(2) !in (high(3) downTo low(1)).reversed()", "HLX") { assertFalse(x(2) !in (high(3) downTo low(1)).reversed()) }
    expectOrder("2 !in (high(3) downTo low(1)).reversed()", "HL") { assertFalse(2 !in (high(3) downTo low(1)).reversed()) }
    expectOrder("x(2) !in high(3) downTo 1", "HX") { assertFalse(x(2) !in high(3) downTo 1) }
    expectOrder("x(2) !in 3 downTo low(1)", "LX") { assertFalse(x(2) !in 3 downTo low(1)) }

    return "OK"
}