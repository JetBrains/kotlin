// "Remove parameter 'x'" "true"
fun <X> foo(<caret>x: X) {}

fun test() {
    foo(x = 1)
}