package test

class MyClass<T>(t: T)

typealias MyAlias<TT> = MyClass<TT>

fun usage() {
    val ref: (String) -> MyAlias<String> = ::<caret>MyAlias
}