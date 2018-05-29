// WITH_RUNTIME
class FooException : Exception()

fun test() {
    <caret>throw FooException()
}