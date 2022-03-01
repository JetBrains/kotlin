// FILE: example1.kt
package foo

open class Base {
    open class Nested
}

class Derived : Base() {
    class NestedInDerived : Nested()
}

// FILE: example2.kt
package bar

open class Base {
    open class Nested
}

class Derived : Base() {
    open class A
    class B : A()

    class Nested : Nested()
}

// FILE: example3.kt

package baz

open class Base {
    open class Nested // (1)

    companion object {
        open class Nested // (2)
    }
}

class Derived : Base() {
    class NestedInDerived : Base.Nested()
}


class A {
    val x = 10

    fun foo() {
        val x = "hello"

        x
        this.x
    }
}
