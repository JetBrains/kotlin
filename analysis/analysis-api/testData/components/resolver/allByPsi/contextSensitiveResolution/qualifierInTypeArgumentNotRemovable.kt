// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
package test

class Foo

sealed class Base<T> {
    class Child<T> : Base<T>()
}

fun usage(b: Base<Foo>) {
    b as Base.Child<Foo>
}
