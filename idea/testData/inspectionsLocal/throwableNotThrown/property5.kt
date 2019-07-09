// PROBLEM: none
class FooException : RuntimeException()

class BarException : RuntimeException()

fun test(i: Int?): RuntimeException {
    val x = i ?: return <caret>FooException()
    return BarException()
}