fun test(
        b: Boolean,
        i: Int
) {
    if (b) {
        when (i) {
            0 -> foo(1)
            else -> null
        }
    } else null
}

fun foo(i: Int) = i

fun box(): String {
    test(true, 1)
    return "OK"
}