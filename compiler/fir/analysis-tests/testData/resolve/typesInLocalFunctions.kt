class Some

fun foo(): () -> Boolean {
    val s = Some()
    if (true) {
        return { if (<!USELESS_IS_CHECK!>s is Some<!>) true else false }
    } else {
        return { true }
    }
}
