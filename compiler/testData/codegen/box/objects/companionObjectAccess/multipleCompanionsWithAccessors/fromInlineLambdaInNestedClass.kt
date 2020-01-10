// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// IGNORE_BACKEND_FIR: JVM_IR
// FILE: fromInlineLambdaInNestedClass.kt
import b.*

fun box() = Outer().test()

// FILE: a.kt
package a

open class A {
    protected companion object {
        val vo = "O"
    }
}

// FILE: b.kt
package b

import a.*

inline fun <T> run(fn: () -> T) = fn()

class Outer : A() {
    private companion object {
        val vk = "K"
    }

    class Nested {
        fun foo() = run { vo + vk }
    }

    fun test() = Nested().foo()
}

