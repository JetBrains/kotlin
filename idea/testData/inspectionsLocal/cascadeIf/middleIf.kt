// PROBLEM: none

fun println(s: String) {}

fun test(a: Boolean, b: Boolean) {
    if (a) {
        println("a")
    }
    else <caret>if (b) {
        println("b")
    }
    else {
        println("none")
    }
}