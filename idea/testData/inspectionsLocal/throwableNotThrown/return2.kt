// PROBLEM: none
class FooException : RuntimeException()

fun createException() = FooException()

fun test() = <caret>createException()