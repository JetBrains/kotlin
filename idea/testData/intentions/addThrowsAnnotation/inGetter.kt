// WITH_RUNTIME

class FooException : Exception()

class Test {
    val getter: String
        get() = <caret>throw FooException()
}