// WITH_RUNTIME

val order = StringBuilder()

inline fun expectOrder(at: String, expected: String, body: () -> Unit) {
    order.setLength(0)
    body()
    if (order.toString() != expected) throw AssertionError("$at: expected: $expected, actual: $order")
}

fun list(): List<Int> {
    order.append("L")
    return emptyList()
}


fun x(i: Int): Int {
    order.append("X")
    return i
}

fun box(): String {
    expectOrder("1 in []", "LX") { x(1) in list() }
    expectOrder("1 !in []", "LX") { x(1) !in list() }

    return "OK"
}