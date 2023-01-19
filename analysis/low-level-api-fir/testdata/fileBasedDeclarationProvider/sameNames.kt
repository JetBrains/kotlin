// CLASS: test/Foo
// CLASS: test/Bar
// CLASS: test/Bar.Baz
// CLASS: test/Bar.Boo
// CLASS: test/Bar.Bok
// FUNCTION: test/#foo
// PROPERTY: test/#foo

package test

class Foo
interface Foo
typealias Foo = Foo
enum class Foo

class Bar {
    class Baz
    object Boo
}

interface Bar {
    object Baz
    interface Bok
}

fun foo() {}
fun foo(a: Int) {}

val foo: String = "foo"