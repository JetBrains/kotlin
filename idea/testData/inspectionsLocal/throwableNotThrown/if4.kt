// PROBLEM: none
class FooException : RuntimeException()

fun createException() = FooException()

fun test(i: Int) {
    val e = if (i == 1) {
        <caret>FooException()
    } else {
        createException()
    }
    throw e
}