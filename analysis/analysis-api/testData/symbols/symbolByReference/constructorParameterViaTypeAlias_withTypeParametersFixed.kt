package test

class MyClass<T>(param: T)

typealias MyAlias = MyClass<String>

fun usage() {
    MyAlias(<caret>param = "hello")
}