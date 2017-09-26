// "Surround with *intArrayOf(...)" "true"
// LANGUAGE_VERSION: 1.2

fun foo(vararg s: Int) {}

fun test() {
    foo(s = <caret>1)
}