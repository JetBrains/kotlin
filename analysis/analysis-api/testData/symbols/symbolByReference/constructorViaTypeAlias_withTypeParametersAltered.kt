// DO_NOT_CHECK_SYMBOL_RESTORE_K1
package test

class MyClass<T>(t: T)

class Cell<T>(t: T)

typealias MyAlias<TT> = MyClass<Cell<TT>>

fun usage() {
    <caret>MyAlias(Cell("hello"))
}