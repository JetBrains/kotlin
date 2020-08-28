// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun box(): String {
    try {
        "a b c".trimMargin(" ")
        return "Fail trimMargin"
    } catch (e: IllegalArgumentException) {
        return "OK"
    }
}
