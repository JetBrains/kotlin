// !DIAGNOSTICS: -UNUSED_EXPRESSION


class Foo<T> {
    class Bar<X> {
        class Baz {

        }
    }
}

fun <T> a() {}

fun test() {
    Foo::class
    Foo.Bar::class
    Foo.Bar.Baz::class

    a<Foo.Bar<String>>()
    a<Foo.Bar.Baz>()

    Foo<String>.Bar::class
    Foo<String>.Bar.Baz::class

    a<Foo<String>.Bar>()
    a<Foo<String>.Bar.Baz>()

    a<Foo.Bar<Int>>()
    a<Foo.Bar<Int>.Baz>()
}

fun <T: Foo<String.Bar>> x() {}
fun Foo<String>.Bar.ext() {}

fun ex1(a: Foo<String>.Bar<String>): Foo<String>.Bar<String> {
}