// WITH_STDLIB

fun box(): String {
    val i = test()
    return if (i != 0)
        "Failed: $i"
    else
        "OK"
}

private fun test(): Int {
    // JS tests fail for array size '1000000000',
    // however, we don't really care much about performance here,
    // but rather check that we generate correct code.
    return "123".indexOfAny(CharArray(100) { '1' })
}
