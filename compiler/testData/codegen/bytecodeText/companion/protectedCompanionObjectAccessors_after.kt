// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// IGNORE_BACKEND: JVM_IR
// ^ TODO implement ProperVisibilityForCompanionObjectInstanceField feature support in JMV_IR

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