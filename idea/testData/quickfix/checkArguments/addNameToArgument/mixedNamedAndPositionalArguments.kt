// "Add name to argument: 'b = "FOO"'" "true"
// LANGUAGE_VERSION: 1.3

fun f(a: Int, b: String) {}

fun g() {
    f(a = 10, <caret>"FOO")
}