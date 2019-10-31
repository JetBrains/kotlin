// FILE: a.kt

package a

object A {
    fun foo() {}
}

// FILE: b.kt

package b
import a.A.foo

fun bar() {
    foo()
}
