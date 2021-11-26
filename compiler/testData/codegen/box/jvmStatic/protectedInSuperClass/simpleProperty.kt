// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// WITH_STDLIB
// FILE: 1.kt
package a

open class A {
    companion object {
        @JvmStatic
        @get:JvmStatic
        @set:JvmStatic
        protected var foo = "Fail"
    }
}

// FILE: 2.kt
import a.*

class B : A() {
    fun bar(): String {
        foo = "OK"
        return foo
    }
}

fun box() = B().bar()
