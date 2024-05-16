// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_K1: JVM_IR
// LANGUAGE: +AllowAccessToProtectedFieldFromSuperCompanion
// WITH_STDLIB
// FILE: A.kt
package a

abstract class A {
    companion object {
        protected const val constVal = "OK"
    }
}

// FILE: B.kt
import a.A

class B : A() {
    fun f() = constVal
}

fun box(): String = B().f()
