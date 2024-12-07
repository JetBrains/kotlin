// DO_NOT_CHECK_SYMBOL_RESTORE_K1
package test

class MyClass

typealias MyAlias = MyClass

fun usage() {
    <caret>MyAlias()
}