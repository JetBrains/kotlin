// PROBLEM: none

fun println(s: String) {}

fun foo(size: Int, a: Boolean, b: Boolean) {
    for (i in 1..size) {
        <caret>if (a) {
            break
        }
        else if (b) {
            println("$i")
        }
        else {
            println("*")
        }
    }
}