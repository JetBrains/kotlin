// LANGUAGE: +AllowAccessToProtectedFieldFromSuperCompanion
// WITH_STDLIB
// FILE: A.kt
package a

abstract class A {
    companion object {
        protected const val constVal = ""

        @JvmField
        protected val jvmFieldVal = ""
    }
}

// FILE: B.kt
import a.A

class B : A() {
    fun f1() = constVal
    fun f2() = jvmFieldVal
}
