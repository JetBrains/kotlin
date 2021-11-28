// TARGET_BACKEND: JVM
// FILE: foo.kt
package foo

abstract class Base {
    protected abstract fun foo(): String
}

// FILE: bar.kt
import foo.*

abstract class C : Base() {
    class A : C() {
        override fun foo() = "OK"
    }

    class B(val x: C) : C() {
        // Needs an accessor (`foo` is in another package and `x` is not assignable to `B`)
        override fun foo() = x.foo()

        fun bar() = foo()
    }
}

fun box() = C.B(C.A()).bar()
