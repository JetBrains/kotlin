// LANGUAGE: +ExplicitBackingFields
package test

open class Base<T> {
    val value: T
        field: Int = 0
        get() = field as T
}

class Derived : Base<String>()

// field: test/Derived.value
