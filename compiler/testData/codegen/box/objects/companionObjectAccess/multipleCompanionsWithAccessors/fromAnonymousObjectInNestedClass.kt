// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// IGNORE_BACKEND_FIR: JVM_IR
// FILE: fromAnonymousObjectInNestedClass.kt
import outer.*

fun box() = Outer().test()

// FILE: Outer.kt
package outer

import a.A

class Outer : A() {
    private companion object {
        fun getK() = "K"
    }

    class Nested {
        fun foo() = object {
            override fun toString() = getO() + getK()
        }
    }

    fun test() = Nested().foo().toString()
}

// FILE: a.kt
package a

open class A {
    protected companion object {
        fun getO() = "O"
    }
}