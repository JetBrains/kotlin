// LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// FILE: twoInheritedProtectedCompanions.kt
import c.C

fun box() = C().test()()

// FILE: a.kt
package a

open class A {
    protected companion object AC {
        fun getO() = "O"
    }
}

// FILE: b.kt
package b

import a.A

open class B : A() {
    protected companion object BC {
        fun getK() = "K"
    }
}

// FILE: c.kt
package c

import a.A
import b.B

class C : B() {
    fun test() = { A.AC.getO() + B.BC.getK() }
}
