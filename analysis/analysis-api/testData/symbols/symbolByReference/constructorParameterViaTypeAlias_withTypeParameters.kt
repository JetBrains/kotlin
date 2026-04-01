package test

class MyClass<T>(param: T)

typealias MyAlias<TT> = MyClass<TT>

fun usage() {
    MyAlias(<caret>param = "hello")
}