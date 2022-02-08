// WITH_STDLIB

fun box(): String {
    try {
        "a b c".trimMargin(" ")
        return "Fail trimMargin"
    } catch (e: IllegalArgumentException) {
        return "OK"
    }
}
