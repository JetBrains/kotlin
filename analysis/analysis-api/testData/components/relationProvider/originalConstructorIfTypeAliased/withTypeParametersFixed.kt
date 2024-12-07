package test

class MyClass<T>(t: T)

typealias MyTypeAlias = MyClass<String>

fun usage() {
    <caret>MyTypeAlias("Hello")
}