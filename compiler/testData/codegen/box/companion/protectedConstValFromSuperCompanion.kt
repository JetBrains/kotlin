// IGNORE_BACKEND_K1: JVM_IR
// LANGUAGE: +AllowAccessToProtectedFieldFromSuperCompanion
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_1_9
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// ^^^ Compiler v2.0.0 does not know this language feature
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
