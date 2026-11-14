// LANGUAGE: +NestedTypeAliases

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
