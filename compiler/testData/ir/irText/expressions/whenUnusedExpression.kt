// WITH_STDLIB

fun test(b: Boolean, i: Int) {
    if (b) {
        when (i) {
            0 -> 1
            else -> null
        }
    } else null
}