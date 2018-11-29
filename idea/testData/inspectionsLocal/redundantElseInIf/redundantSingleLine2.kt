// WITH_RUNTIME
class SomeException : RuntimeException()
fun foo(): Int = 1

fun test(x: Int): Int {
    if (x == 1) {
        throw SomeException()
    } else if (x == 2) {
        throw SomeException()
    } <caret>else return foo()
}