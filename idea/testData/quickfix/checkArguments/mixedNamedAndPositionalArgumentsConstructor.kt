// "Add name to argument: 'b = "FOO"'" "true"
class A(a: Int, b: String) {}

fun f() {
     A(a = 1, <caret>"FOO")
}