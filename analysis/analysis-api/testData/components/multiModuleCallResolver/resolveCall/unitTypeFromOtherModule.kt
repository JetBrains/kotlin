// MODULE: lib
package dependency

abstract class A<T> {
    abstract fun foo()
}

// MODULE: main(lib)
// FILE: B.kt
package main
import c.C
import dependency.A

abstract class B : A<C>() {}

fun usage(b : B?) {
    if (b != null) {
        b.f<caret>oo()
    }
}
// FILE: C.kt
package c
class C