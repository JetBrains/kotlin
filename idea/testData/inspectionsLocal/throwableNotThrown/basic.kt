// PROBLEM: Throwable instance 'FooException' is not thrown
// FIX: none
class FooException : RuntimeException()

fun test() {
    <caret>FooException()
}