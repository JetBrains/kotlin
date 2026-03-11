package test

class MyClass(param: String)

typealias MyAlias = MyClass

fun usage() {
    MyAlias(<caret>param = "hello")
}