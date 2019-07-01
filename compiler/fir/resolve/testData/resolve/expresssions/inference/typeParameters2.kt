interface Foo
class FooImpl : Foo
class FooBarImpl : Foo

fun <T : Foo> foo(t: T) = t


fun main(fooImpl: FooImpl, fooBarImpl: FooBarImpl) {
    val a = foo<FooImpl>(fooBarImpl)
    val b = foo<Foo>(fooImpl)
}