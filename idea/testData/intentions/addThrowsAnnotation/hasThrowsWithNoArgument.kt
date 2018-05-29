// WITH_RUNTIME
class FooException : Exception()

@Throws
fun test() {
    throw FooException()<caret>
}