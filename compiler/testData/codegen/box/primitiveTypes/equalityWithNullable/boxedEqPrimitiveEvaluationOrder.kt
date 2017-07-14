var order: String = ""

fun a(i: Int): Int? {
    order += "a"
    return i
}

fun b(i: Int): Int {
    order += "b"
    return i
}

inline fun evaluateAndCheckOrder(marker: String, expectedValue: Boolean, expectedOrder: String, expr: () -> Boolean) {
    order = ""
    val actualValue = expr()
    if (actualValue != expectedValue) throw AssertionError("$marker: Expected: $expectedValue, actual: $actualValue")
    if (order != expectedOrder) throw AssertionError("$marker, order: Expected: '$expectedOrder', actual: '$order'")
}

val nn: Int? = null

fun box(): String {
    evaluateAndCheckOrder("1", true, "ab") { a(1) == b(1) }
    evaluateAndCheckOrder("2", true, "ab") { a(1) != b(2) }
    evaluateAndCheckOrder("3", true, "ab") { !(a(1) == b(2)) }
    evaluateAndCheckOrder("4", true, "ab") { !(a(1) != b(1)) }

    evaluateAndCheckOrder("null == 1", false, "a") { nn == a(1) }
    evaluateAndCheckOrder("null != 1", true, "a") { nn != a(1) }
    evaluateAndCheckOrder("!(null == 1)", true, "a") { !(nn == a(1)) }
    evaluateAndCheckOrder("!(null != 1)", false, "a") { !(nn != a(1)) }

    return "OK"
}