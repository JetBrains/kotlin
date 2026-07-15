package test

open class Base<T> {
    open fun foo(t: T): T = t
}

class Derived : Base<String>()

// value_parameter: t: function: test/Derived.foo
