package test

class MyClass<T>(t: T)

typealias MyTypeAlias<TT> = MyClass<TT>

fun usage() {
    <caret>MyTypeAlias("Hello")
}