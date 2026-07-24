// LANGUAGE: +ExplicitBackingFields
package test

interface A {
    val value: Int
}

open class B {
    val value: Int
        field = 0
}

class C : B(), A

// field: test/C.value
