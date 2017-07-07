// WITH_RUNTIME

val order = StringBuilder()

inline fun expectOrder(at: String, expected: String, body: () -> Unit) {
    order.setLength(0)
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
    expectOrder("0 in 1 .. 3", "HLX") { x(0) in high(3) downTo low(1) }
    expectOrder("0 !in 1 .. 3", "HLX") { x(0) !in high(3) downTo low(1) }

    return "OK"
}