// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
package test

class MyClass<T>(t: T)

typealias MyAlias = MyClass<String>

fun usage() {
    <caret>MyAlias("hello")
}