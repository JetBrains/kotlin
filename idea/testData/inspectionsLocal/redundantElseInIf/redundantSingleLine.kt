// WITH_RUNTIME
class SomeException : RuntimeException()
fun foo(): Int = 1

fun test(x: Boolean): Int {
    if (x) throw SomeException() else<caret> return foo()
}