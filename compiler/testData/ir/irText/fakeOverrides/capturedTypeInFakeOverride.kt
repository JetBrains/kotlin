// FIR_IDENTICAL
// WITH_STDLIB

open class Base<T> {
    fun foo(t: T) = Unit
}

open class Intermediate<S> : Base<S>()

class Foo<R> : Intermediate<R>() {}
