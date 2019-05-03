// PROBLEM: none
class FooException : RuntimeException()

fun createException() = FooException()

fun test(i: Int) {
    throw if (i == 1) {
        <caret>FooException()
    } else {
        createException()
    }
}