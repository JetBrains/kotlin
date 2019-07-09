// PROBLEM: none
fun <T> foo(t: T) {}

fun test() {
    foo({ <caret>_: Boolean -> "" })
}