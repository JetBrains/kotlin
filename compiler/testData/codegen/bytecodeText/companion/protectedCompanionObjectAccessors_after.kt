// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// LAMBDAS: CLASS
// FILE: Base.kt
package a

open class Base {
    protected companion object {
        fun foo() = 1
    }
}

// FILE: Host.kt
import a.*

class Host : Base() {
    fun test() = { foo() }
}

// @Host.class:
// 1 synthetic access\$
