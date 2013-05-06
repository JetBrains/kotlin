// "Add name to argument: 'b = "FOO"'" "true"
fun f(a: String, x: Int, b: String) {}

fun g() {
    f("BAR", x = 10, <caret>"FOO")
}