package test

class GenericClass<T>(t: T)

class MyClass

typealias TypeAlias<TT> = GenericClass<TT>

fun usage() {
    <expr>TypeAlias(MyClass())</expr>
}
