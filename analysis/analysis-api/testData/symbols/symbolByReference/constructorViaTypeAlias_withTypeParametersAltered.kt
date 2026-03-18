// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
package test

class MyClass<T>(t: T)

class Cell<T>(t: T)

typealias MyAlias<TT> = MyClass<Cell<TT>>

fun usage() {
    <caret>MyAlias(Cell("hello"))
}