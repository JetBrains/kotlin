// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// WITH_STDLIB
// FILE: 1.kt
package a

open class A {
    companion object {
        @JvmStatic // Required to be accessible from subclasses of A in other packages.
        protected fun foo() = "OK"
    }
}

// FILE: 2.kt
import a.*

class B : A() {
    fun bar() = foo() // calls static A.foo(), not inaccessible A.Companion.foo()
}

fun box() = B().bar()
