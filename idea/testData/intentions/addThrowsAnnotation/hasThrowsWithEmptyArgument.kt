// WITH_RUNTIME
class FooException : Exception()

@Throws()
fun test() {
    <caret>throw FooException()
}