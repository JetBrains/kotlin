// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
package test

class MyClass<T>(t: T)

typealias MyAlias<TT> = MyClass<TT>

fun usage() {
    val reference: (String) -> MyAlias<String> = ::<caret>MyAlias
}