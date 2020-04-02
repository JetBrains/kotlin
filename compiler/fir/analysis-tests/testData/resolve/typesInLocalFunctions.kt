class Some

fun foo(): () -> Boolean {
    val s = Some()
    if (true) {
        return { if (s is Some) true else false }
    } else {
        return { true }
    }
}