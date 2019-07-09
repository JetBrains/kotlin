// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

var order = StringBuilder()

inline fun expectOrder(at: String, expected: String, body: () -> Unit) {
    order = StringBuilder() // have to do that in order to run this test in JS
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