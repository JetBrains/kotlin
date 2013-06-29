// "Add name to argument: 'b = "FOO"'" "true"
fun f(a: Int, b: String) {}

fun g() {
    f(a = 10, <caret>"FOO")
}