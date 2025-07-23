// LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// FILE: a.kt
package a

open class A {
    protected companion object {
        fun getO() = "O"
    }
}

// FILE: b.kt
package b

import a.*

class Outer : A() {
    private companion object {
        fun getK() = "K"
    }

    class Nested {
        val test: String

        init {
            test = getO() + getK()
        }
    }
}

// FILE: fromInitBlockOfNestedClass.kt
import b.*

fun box() = Outer.Nested().test
