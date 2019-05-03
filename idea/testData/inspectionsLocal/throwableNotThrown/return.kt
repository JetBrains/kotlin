// PROBLEM: none
class FooException : RuntimeException()

fun createException() = FooException()

fun test(): FooException {
    return <caret>createException()
}