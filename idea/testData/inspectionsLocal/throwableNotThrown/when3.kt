// PROBLEM: Result of 'createException' call is not thrown
// FIX: none
class FooException : RuntimeException()

fun createException() = FooException()

fun test(i: Int) {
    val e = when (i) {
        1 -> FooException()
        else -> <caret>createException()
    }
}