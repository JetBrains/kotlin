// KJS_WITH_FULL_RUNTIME
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63864

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

fun minValue() = Int.MIN_VALUE

fun box(): String {
    expectOrder("x(0) in low(1) until high(3)", "LHX") { assertFalse(x(0) in low(1) until high(3)) }
    expectOrder("0 in low(1) until high(3)", "LH") { assertFalse(0 in low(1) until high(3)) }
    expectOrder("x(0) in 1 until high(3)", "HX") { assertFalse(x(0) in 1 until high(3)) }
    expectOrder("x(0) in low(1) until 3", "LX") { assertFalse(x(0) in low(1) until 3) }
    expectOrder("x(0) !in low(1) until high(3)", "LHX") { assertTrue(x(0) !in low(1) until high(3)) }
    expectOrder("0 !in low(1) until high(3)", "LH") { assertTrue(0 !in low(1) until high(3)) }
    expectOrder("x(0) !in 1 until high(3)", "HX") { assertTrue(x(0) !in 1 until high(3)) }
    expectOrder("x(0) !in low(1) until 3", "LX") { assertTrue(x(0) !in low(1) until 3) }

    expectOrder("x(4) in low(1) until high(3)", "LHX") { assertFalse(x(4) in low(1) until high(3)) }
    expectOrder("4 in low(1) until high(3)", "LH") { assertFalse(4 in low(1) until high(3)) }
    expectOrder("x(4) in 1 until high(3)", "HX") { assertFalse(x(4) in 1 until high(3)) }
    expectOrder("x(4) in low(1) until 3", "LX") { assertFalse(x(4) in low(1) until 3) }
    expectOrder("x(4) !in low(1) until high(3)", "LHX") { assertTrue(x(4) !in low(1) until high(3)) }
    expectOrder("4 !in low(1) until high(3)", "LH") { assertTrue(4 !in low(1) until high(3)) }
    expectOrder("x(4) !in 1 until high(3)", "HX") { assertTrue(x(4) !in 1 until high(3)) }
    expectOrder("x(4) !in low(1) until 3", "LX") { assertTrue(x(4) !in low(1) until 3) }

    expectOrder("x(2) in low(1) until high(3)", "LHX") { assertTrue(x(2) in low(1) until high(3)) }
    expectOrder("2 in low(1) until high(3)", "LH") { assertTrue(2 in low(1) until high(3)) }
    expectOrder("x(2) in 1 until high(3)", "HX") { assertTrue(x(2) in 1 until high(3)) }
    expectOrder("x(2) in low(1) until 3", "LX") { assertTrue(x(2) in low(1) until 3) }
    expectOrder("x(2) !in low(1) until high(3)", "LHX") { assertFalse(x(2) !in low(1) until high(3)) }
    expectOrder("2 !in low(1) until high(3)", "LH") { assertFalse(2 !in low(1) until high(3)) }
    expectOrder("x(2) !in 1 until high(3)", "HX") { assertFalse(x(2) !in 1 until high(3)) }
    expectOrder("x(2) !in low(1) until 3", "LX") { assertFalse(x(2) !in low(1) until 3) }

    // For IR backend, there is an additional condition for checking the upper bound == MIN_VALUE (empty range).
    // These tests ensure all expressions are evaluated and in the correct order.
    expectOrder("0 in low(1) until Int.MIN_VALUE", "L") { assertFalse(0 in low(1) until Int.MIN_VALUE) }
    expectOrder("0 in low(1) until minValue()", "L") { assertFalse(0 in low(1) until minValue()) }
    expectOrder("x(0) in 1 until Int.MIN_VALUE", "X") { assertFalse(x(0) in 1 until Int.MIN_VALUE) }
    expectOrder("x(0) in 1 until minValue()", "X") { assertFalse(x(0) in 1 until minValue()) }
    expectOrder("x(0) in low(1) until Int.MIN_VALUE", "LX") { assertFalse(x(0) in low(1) until Int.MIN_VALUE) }
    expectOrder("x(0) in low(1) until minValue()", "LX") { assertFalse(x(0) in low(1) until minValue()) }
    expectOrder("0 !in low(1) until Int.MIN_VALUE", "L") { assertTrue(0 !in low(1) until Int.MIN_VALUE) }
    expectOrder("0 !in low(1) until minValue()", "L") { assertTrue(0 !in low(1) until minValue()) }
    expectOrder("x(0) !in 1 until Int.MIN_VALUE", "X") { assertTrue(x(0) !in 1 until Int.MIN_VALUE) }
    expectOrder("x(0) !in 1 until minValue()", "X") { assertTrue(x(0) !in 1 until minValue()) }
    expectOrder("x(0) !in low(1) until Int.MIN_VALUE", "LX") { assertTrue(x(0) !in low(1) until Int.MIN_VALUE) }
    expectOrder("x(0) !in low(1) until minValue()", "LX") { assertTrue(x(0) !in low(1) until minValue()) }

    return "OK"
}