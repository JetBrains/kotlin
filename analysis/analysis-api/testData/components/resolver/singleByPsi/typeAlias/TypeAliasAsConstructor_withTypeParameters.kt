// IGNORE_STABILITY_K2: call
package test

class MyClass<T>(t: T)

typealias MyAlias<TT> = MyClass<TT>

fun usage() {
    <caret>MyAlias("hello")
}