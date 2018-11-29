// PROBLEM: none
// WITH_RUNTIME
class SomeException : RuntimeException()
fun foo(): Int = 1
fun bar(): Int = 2

fun test(x: Boolean, y: Boolean) {
    if (x) {
        foo()
    } else if (y) {
        throw SomeException()
    } else<caret> {
        foo()
        bar()
    }
}