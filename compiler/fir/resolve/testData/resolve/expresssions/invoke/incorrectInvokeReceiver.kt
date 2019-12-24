fun String.invoke() = this

val some = ""
fun sss() {
    val some = 10

    <!INAPPLICABLE_CANDIDATE!>some<!>() // Should be inapplicable
}