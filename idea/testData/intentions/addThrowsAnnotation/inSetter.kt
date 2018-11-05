// WITH_RUNTIME

class FooException : Exception()

class Test {
    var setter: String = ""
        set(value) = <caret>throw FooException()
}