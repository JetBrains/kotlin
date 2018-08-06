// PROBLEM: none
fun foo(): Int = 1
fun bar(): Int = 2

fun test(x: Boolean, y: Boolean) {
    if (x) foo()
    else if (y) return
    else<caret> bar()
}