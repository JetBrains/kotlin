// ISSUE: KT-62783

abstract class Foo<T>

class FooBar : Foo<Any>()

fun test1(value: FooBar) {
    value as Foo<*>
    value as? Foo<*>
    value as Foo<*>?
    value as? Foo<*>?
}

fun test2(value: FooBar?) {
    value as Foo<*>
    value as? Foo<*>
    value as Foo<*>?
    value as? Foo<*>?
}
