// WITH_STDLIB

fun box(): String {
    try {
        sequenceOf(1, 2, 3).take(-1)
    } catch (e: IllegalArgumentException) {
        return "OK"
    }
    return "failed: take(-1) didn't throw an exception"
}
