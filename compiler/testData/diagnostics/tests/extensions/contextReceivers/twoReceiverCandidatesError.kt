// LANGUAGE: +ContextReceivers
// FIR_IDENTICAL

fun String.foo() {}

context(Int, Double)
fun bar() {
    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>() // should be prohibited
}

fun main() {
    with(1) {
        with(2.0) {
            bar()
        }
    }
}
