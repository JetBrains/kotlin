// DO_NOT_CHECK_SYMBOL_RESTORE_K1
package test

class MyClass<T>(t: T)

typealias MyAlias = MyClass<String>

fun usage() {
    <caret>MyAlias("hello")
}