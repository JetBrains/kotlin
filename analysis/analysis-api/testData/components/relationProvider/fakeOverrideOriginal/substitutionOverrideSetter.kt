package test

open class Base<T> {
    open var value: T
        get() = null!!
        set(value) {}
}

class Derived : Base<String>()

// setter: property: test/Derived.value
