// "Add name to argument..." "true"
// LANGUAGE_VERSION: 1.3

fun f(a: Int, b: String = "b", c: String = "c") {}

fun g() {
    f(a = 10, <caret>"FOO")
}