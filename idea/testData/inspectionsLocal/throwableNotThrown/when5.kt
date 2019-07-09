// PROBLEM: none
class FooException : RuntimeException()

fun createException() = FooException()

fun test(i: Int) {
    val x = when (i) {
        1 -> 10
        else -> throw <caret>createException()
    }
}