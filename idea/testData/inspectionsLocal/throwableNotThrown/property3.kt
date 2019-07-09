// PROBLEM: none
class FooException : Exception()

fun test() {
    val e = <caret>FooException()
    val e2 = e
}