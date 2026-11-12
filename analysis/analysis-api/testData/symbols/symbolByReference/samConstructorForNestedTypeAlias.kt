// LANGUAGE: +NestedTypeAliases
// DO_NOT_REQUIRE_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION

package test

fun interface MyInterface {
    fun execute()
}

class Outer {
    typealias MyTypeAlias = MyInterface
}

fun usage() {
    Outer.MyType<caret>Alias { }
}
