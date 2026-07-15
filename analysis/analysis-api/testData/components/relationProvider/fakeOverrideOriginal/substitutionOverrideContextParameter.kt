package test

open class Base<T> {
    context(c: T)
    open fun foo() {}
}

class Derived : Base<String>()

// context_parameter: c: function: test/Derived.foo
