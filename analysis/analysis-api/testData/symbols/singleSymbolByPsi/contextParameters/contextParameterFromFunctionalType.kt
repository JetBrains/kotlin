// ILLEGAL_PSI

fun check() {
    val i: context(s: <caret>String) Int.() -> Unit = { }
}
