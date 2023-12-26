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
    expectOrder("x(0) in (low(1) until high(3)).reversed()", "LHX") { assertFalse(x(0) in (low(1) until high(3)).reversed()) }
    expectOrder("0 in (low(1) until high(3)).reversed()", "LH") { assertFalse(0 in (low(1) until high(3)).reversed()) }
    expectOrder("x(0) in 1 until high(3)", "HX") { assertFalse(x(0) in 1 until high(3)) }
    expectOrder("x(0) in low(1) until 3", "LX") { assertFalse(x(0) in low(1) until 3) }
    expectOrder("x(0) !in (low(1) until high(3)).reversed()", "LHX") { assertTrue(x(0) !in (low(1) until high(3)).reversed()) }
    expectOrder("0 !in (low(1) until high(3)).reversed()", "LH") { assertTrue(0 !in (low(1) until high(3)).reversed()) }
    expectOrder("x(0) !in 1 until high(3)", "HX") { assertTrue(x(0) !in 1 until high(3)) }
    expectOrder("x(0) !in low(1) until 3", "LX") { assertTrue(x(0) !in low(1) until 3) }

    expectOrder("x(4) in (low(1) until high(3)).reversed()", "LHX") { assertFalse(x(4) in (low(1) until high(3)).reversed()) }
    expectOrder("4 in (low(1) until high(3)).reversed()", "LH") { assertFalse(4 in (low(1) until high(3)).reversed()) }
    expectOrder("x(4) in 1 until high(3)", "HX") { assertFalse(x(4) in 1 until high(3)) }
    expectOrder("x(4) in low(1) until 3", "LX") { assertFalse(x(4) in low(1) until 3) }
    expectOrder("x(4) !in (low(1) until high(3)).reversed()", "LHX") { assertTrue(x(4) !in (low(1) until high(3)).reversed()) }
    expectOrder("4 !in (low(1) until high(3)).reversed()", "LH") { assertTrue(4 !in (low(1) until high(3)).reversed()) }
    expectOrder("x(4) !in 1 until high(3)", "HX") { assertTrue(x(4) !in 1 until high(3)) }
    expectOrder("x(4) !in low(1) until 3", "LX") { assertTrue(x(4) !in low(1) until 3) }

    expectOrder("x(2) in (low(1) until high(3)).reversed()", "LHX") { assertTrue(x(2) in (low(1) until high(3)).reversed()) }
    expectOrder("2 in (low(1) until high(3)).reversed()", "LH") { assertTrue(2 in (low(1) until high(3)).reversed()) }
    expectOrder("x(2) in 1 until high(3)", "HX") { assertTrue(x(2) in 1 until high(3)) }
    expectOrder("x(2) in low(1) until 3", "LX") { assertTrue(x(2) in low(1) until 3) }
    expectOrder("x(2) !in (low(1) until high(3)).reversed()", "LHX") { assertFalse(x(2) !in (low(1) until high(3)).reversed()) }
    expectOrder("2 !in (low(1) until high(3)).reversed()", "LH") { assertFalse(2 !in (low(1) until high(3)).reversed()) }
    expectOrder("x(2) !in 1 until high(3)", "HX") { assertFalse(x(2) !in 1 until high(3)) }
    expectOrder("x(2) !in low(1) until 3", "LX") { assertFalse(x(2) !in low(1) until 3) }

    // For IR backend, there is an additional condition for checking the upper bound == MIN_VALUE (empty range).
    // These tests ensure all expressions are evaluated and in the correct order.
    expectOrder("0 in (low(1) until Int.MIN_VALUE).reversed()", "L") { assertFalse(0 in (low(1) until Int.MIN_VALUE).reversed()) }
    expectOrder("0 in (low(1) until minValue()).reversed()", "L") { assertFalse(0 in (low(1) until minValue()).reversed()) }
    expectOrder("x(0) in (1 until Int.MIN_VALUE).reversed()", "X") { assertFalse(x(0) in (1 until Int.MIN_VALUE).reversed()) }
    expectOrder("x(0) in (1 until minValue()).reversed()", "X") { assertFalse(x(0) in (1 until minValue()).reversed()) }
    expectOrder("x(0) in (low(1) until Int.MIN_VALUE).reversed()", "LX") { assertFalse(x(0) in (low(1) until Int.MIN_VALUE).reversed()) }
    expectOrder("x(0) in (low(1) until minValue()).reversed()", "LX") { assertFalse(x(0) in (low(1) until minValue()).reversed()) }
    expectOrder("0 !in (low(1) until Int.MIN_VALUE).reversed()", "L") { assertTrue(0 !in (low(1) until Int.MIN_VALUE).reversed()) }
    expectOrder("0 !in (low(1) until minValue()).reversed()", "L") { assertTrue(0 !in (low(1) until minValue()).reversed()) }
    expectOrder("x(0) !in (1 until Int.MIN_VALUE).reversed()", "X") { assertTrue(x(0) !in (1 until Int.MIN_VALUE).reversed()) }
    expectOrder("x(0) !in (1 until minValue()).reversed()", "X") { assertTrue(x(0) !in (1 until minValue()).reversed()) }
    expectOrder("x(0) !in (low(1) until Int.MIN_VALUE).reversed()", "LX") { assertTrue(x(0) !in (low(1) until Int.MIN_VALUE).reversed()) }
    expectOrder("x(0) !in (low(1) until minValue()).reversed()", "LX") { assertTrue(x(0) !in (low(1) until minValue()).reversed()) }

    return "OK"
}