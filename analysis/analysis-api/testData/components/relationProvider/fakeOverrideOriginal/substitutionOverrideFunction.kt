package test

open class Base<T> {
    open fun foo(t: T): T = t
}

class Derived : Base<String>()

// function: test/Derived.foo
