package test

open class Base<T> {
    open val value: T get() = null!!
}

class Derived : Base<String>()

// property: test/Derived.value
