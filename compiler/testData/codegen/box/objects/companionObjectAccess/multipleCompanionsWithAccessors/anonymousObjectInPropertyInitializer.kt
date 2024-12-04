// LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// FILE: anonymousObjectInPropertyInitializer.kt
import c.C

fun box() = C().test.toString()

// FILE: a.kt
package a

open class A {
    protected companion object {
        fun getO() = "O"
    }
}

// FILE: b.kt
package b

import a.A

open class B : A() {
    protected companion object {
        fun getK() = "K"
    }
}

// FILE: c.kt
package c

import b.B

class C {
    val test = object : B() {
        override fun toString() = getO() + getK()
    }
}
