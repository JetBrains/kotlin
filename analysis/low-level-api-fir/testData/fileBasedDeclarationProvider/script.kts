// CLASS: test/app/Foo
// CLASS: test/app/Foo.Bar
// CLASS: test/app/Foo.Bar.Baz
// TYPE_ALIAS: test/app/MyString
// FUNCTION: test/app/#foo
// PROPERTY: test/app/#bar

package test.app

interface Foo {
    class Bar {
        object Baz
    }

    fun nested() {}
}

typealias MyString = String

fun foo() {}

val bar: String = "bar"

foo()