// PROBLEM: none

fun println(s: String) {}

fun foo(a: Boolean, b: Boolean) {
    <caret>when ("") {
        "a" -> println("a")
        else -> println("else")

    }
}