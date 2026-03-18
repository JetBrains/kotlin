// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
package test

class MyClass

typealias MyAlias = MyClass

fun usage() {
    <caret>MyAlias()
}