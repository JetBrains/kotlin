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

fun x(i: Int?): Int? {
    order.append("X")
    return i
}

fun box(): String {
    expectOrder("x(null) in low(1) .. high(3)", "LHX") { assertFalse(x(null) in low(1) .. high(3)) }
    expectOrder("null in low(1) .. high(3)", "LH") { assertFalse(null in low(1) .. high(3)) }
    expectOrder("x(null) in 1 .. high(3)", "HX") { assertFalse(x(null) in 1 .. high(3)) }
    expectOrder("x(null) in low(1) .. 3", "LX") { assertFalse(x(null) in low(1) .. 3) }
    expectOrder("x(null) !in low(1) .. high(3)", "LHX") { assertTrue(x(null) !in low(1) .. high(3)) }
    expectOrder("null !in low(1) .. high(3)", "LH") { assertTrue(null !in low(1) .. high(3)) }
    expectOrder("x(null) !in 1 .. high(3)", "HX") { assertTrue(x(null) !in 1 .. high(3)) }
    expectOrder("x(null) !in low(1) .. 3", "LX") { assertTrue(x(null) !in low(1) .. 3) }

    expectOrder("x(0) in low(1) .. high(3)", "LHX") { assertFalse(x(0) in low(1) .. high(3)) }
    expectOrder("0 in low(1) .. high(3)", "LH") { assertFalse(0 in low(1) .. high(3)) }
    expectOrder("x(0) in 1 .. high(3)", "HX") { assertFalse(x(0) in 1 .. high(3)) }
    expectOrder("x(0) in low(1) .. 3", "LX") { assertFalse(x(0) in low(1) .. 3) }
    expectOrder("x(0) !in low(1) .. high(3)", "LHX") { assertTrue(x(0) !in low(1) .. high(3)) }
    expectOrder("0 !in low(1) .. high(3)", "LH") { assertTrue(0 !in low(1) .. high(3)) }
    expectOrder("x(0) !in 1 .. high(3)", "HX") { assertTrue(x(0) !in 1 .. high(3)) }
    expectOrder("x(0) !in low(1) .. 3", "LX") { assertTrue(x(0) !in low(1) .. 3) }

    expectOrder("x(4) in low(1) .. high(3)", "LHX") { assertFalse(x(4) in low(1) .. high(3)) }
    expectOrder("4 in low(1) .. high(3)", "LH") { assertFalse(4 in low(1) .. high(3)) }
    expectOrder("x(4) in 1 .. high(3)", "HX") { assertFalse(x(4) in 1 .. high(3)) }
    expectOrder("x(4) in low(1) .. 3", "LX") { assertFalse(x(4) in low(1) .. 3) }
    expectOrder("x(4) !in low(1) .. high(3)", "LHX") { assertTrue(x(4) !in low(1) .. high(3)) }
    expectOrder("4 !in low(1) .. high(3)", "LH") { assertTrue(4 !in low(1) .. high(3)) }
    expectOrder("x(4) !in 1 .. high(3)", "HX") { assertTrue(x(4) !in 1 .. high(3)) }
    expectOrder("x(4) !in low(1) .. 3", "LX") { assertTrue(x(4) !in low(1) .. 3) }

    expectOrder("x(2) in low(1) .. high(3)", "LHX") { assertTrue(x(2) in low(1) .. high(3)) }
    expectOrder("2 in low(1) .. high(3)", "LH") { assertTrue(2 in low(1) .. high(3)) }
    expectOrder("x(2) in 1 .. high(3)", "HX") { assertTrue(x(2) in 1 .. high(3)) }
    expectOrder("x(2) in low(1) .. 3", "LX") { assertTrue(x(2) in low(1) .. 3) }
    expectOrder("x(2) !in low(1) .. high(3)", "LHX") { assertFalse(x(2) !in low(1) .. high(3)) }
    expectOrder("2 !in low(1) .. high(3)", "LH") { assertFalse(2 !in low(1) .. high(3)) }
    expectOrder("x(2) !in 1 .. high(3)", "HX") { assertFalse(x(2) !in 1 .. high(3)) }
    expectOrder("x(2) !in low(1) .. 3", "LX") { assertFalse(x(2) !in low(1) .. 3) }

    return "OK"
}