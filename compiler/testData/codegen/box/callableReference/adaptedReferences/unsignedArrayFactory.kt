// KT-89919
// WITH_STDLIB

fun box(): String {
    val fn = ::uintArrayOf
    val result = fn(uintArrayOf(42u))

    if (result[0] != 42u) return "Fail: 42 is expected, got ${result[0]}"

    return "OK"
}
