// PROBLEM: none
// WITH_RUNTIME
class SomeException : RuntimeException()
fun foo(): Int = 1

fun test(x: Boolean, y: Boolean) {
    val i: Int = if (x) throw SomeException()
    else if (y) return
    else<caret> foo()
}