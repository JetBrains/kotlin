// WITH_RUNTIME
// IS_APPLICABLE: false

class FooException : Exception()

@Throws(exceptionClasses = arrayOf(FooException::class))
fun test() {
    <caret>throw FooException()
}