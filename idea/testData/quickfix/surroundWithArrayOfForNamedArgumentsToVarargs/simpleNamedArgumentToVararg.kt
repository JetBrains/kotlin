// "Surround with *arrayOf(...)" "true"
// LANGUAGE_VERSION: 1.2

fun foo(vararg s: String) {}

fun test() {
    foo(s = <caret>"value")
}