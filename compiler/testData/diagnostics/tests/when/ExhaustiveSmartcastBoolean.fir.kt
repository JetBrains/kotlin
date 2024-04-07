// ISSUE: KT-24807

fun testNullableAnyToBoolean(x: Any?) {
    if (x !is Boolean) return
    return when (x) {
        true -> Unit
        false -> Unit
    }
}

fun testAnyToBoolean(x: Any) {
    if (x !is Boolean) return
    return when (x) {
        true -> Unit
        false -> Unit
    }
}
