// "Add name to argument: 'b = "FOO"'" "true"
// LANGUAGE_VERSION: 1.3

fun f(a: String, x: Int, b: String) {}

fun g() {
    f("BAR", x = 10, <caret>"FOO")
}