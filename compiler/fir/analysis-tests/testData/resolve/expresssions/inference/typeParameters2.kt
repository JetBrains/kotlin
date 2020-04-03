interface Foo
class FooImpl : Foo
class FooBarImpl : Foo

fun <T : Foo> foo(t: T) = t


fun main(fooImpl: FooImpl, fooBarImpl: FooBarImpl) {
    val a = <!INAPPLICABLE_CANDIDATE!>foo<!><FooImpl>(fooBarImpl)
    val b = foo<Foo>(fooImpl)
}