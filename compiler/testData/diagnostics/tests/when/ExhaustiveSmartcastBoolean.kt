// ISSUE: KT-24807

fun testNullableAnyToBoolean(x: Any?) {
    if (x !is Boolean) return
    return when (<!DEBUG_INFO_SMARTCAST!>x<!>) {
        true -> Unit
        false -> Unit
    }
}

fun testAnyToBoolean(x: Any) {
    if (x !is Boolean) return
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        true -> Unit
        false -> Unit
    }
}
