interface Foo
class FooImpl : Foo
class Bar

fun <T : Foo> foo(t: T) = t


fun main(fooImpl: FooImpl, bar: Bar) {
    val a = foo(fooImpl)
    val b = <!INAPPLICABLE_CANDIDATE{LT}!><!INAPPLICABLE_CANDIDATE{PSI}!>foo<!>(bar)<!>
}
