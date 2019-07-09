// WITH_RUNTIME
class FooException : Exception()

class BarException : Exception()

@Throws(BarException::class)
fun test() {
    <caret>throw FooException()
}