// LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// FILE: a.kt
package a

open class A {
    protected companion object {
        fun getOK() = "OK"
    }
}

// FILE: b.kt
package b

import a.*

class B {
    companion object : A() {
        val ok = getOK()
    }
}

// FILE: withCompanionObjectBase.kt
import b.*

fun box() = B.ok
