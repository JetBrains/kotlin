// "Surround with *arrayOf(...)" "true"
// LANGUAGE_VERSION: 1.2

fun anyFoo(vararg a: Any) {}

fun test() {
    anyFoo(a = intArr<caret>ayOf(1))
}