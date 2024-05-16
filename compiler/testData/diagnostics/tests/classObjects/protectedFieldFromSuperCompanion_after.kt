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
    fun f1() = <!SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC!>constVal<!>
    fun f2() = <!SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC!>jvmFieldVal<!>
}
