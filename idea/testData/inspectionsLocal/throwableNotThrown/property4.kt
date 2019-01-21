// PROBLEM: none
class FooException : RuntimeException()

fun test(i: Int?) {
    val x = i ?: throw <caret>FooException()
}