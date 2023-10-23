// ISSUE: KT-42169

open class Foo<T>
class Foo1<A> : Foo<Int>()
class Foo2 : Foo<Int>()

fun process(foo: Foo<Long>) {
    foo as Foo1<*>
}
