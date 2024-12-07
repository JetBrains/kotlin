// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// LANGUAGE: +AllowAccessToProtectedFieldFromSuperCompanion
// WITH_STDLIB
// FILE: A.kt
package a

abstract class A {
    companion object {
        @JvmField
        protected val jvmFieldVal = "OK"
    }
}

// FILE: B.kt
import a.A

class B : A() {
    fun f() = jvmFieldVal
}

fun box(): String = B().f()
