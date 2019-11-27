// IGNORE_BACKEND_FIR: JVM_IR
// FILE: 1.kt

import b.B
import a.BSamePackage

fun box() = if (B().test() == BSamePackage().test()) "OK" else "fail"

// FILE: 2.kt

package a

open class A {
    protected fun protectedFun(): String = "OK"
}

class BSamePackage: A() {
    fun test(): String {
        val a = {
            protectedFun()
        }
        return a()
    }
}

// FILE: 3.kt

package b

import a.A

class B: A() {
    fun test(): String {
        val a = {
            protectedFun()
        }
        return a()
    }
}
