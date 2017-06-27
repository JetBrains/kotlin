// PROBLEM: none

fun println(s: String) {}

fun foo(a: Boolean, b: Boolean) {
    <caret>if (a) {
        if (b) println("ab")
        else println("a")
    }
    else if (b) {
        println("b")
    }
    else {
        println("none")
    }
}