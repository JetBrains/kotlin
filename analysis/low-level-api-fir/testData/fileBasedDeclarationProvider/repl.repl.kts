// CLASS: test/app/Repl_repl.Foo
// CLASS: test/app/Repl_repl.Foo.Bar
// CLASS: test/app/Repl_repl.Foo.Bar.Baz
// TYPE_ALIAS: test/app/Repl_repl.MyString

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
