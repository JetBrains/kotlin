// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// FILE: inheritedProtectedCompanionAndOwnPrivateCompanion.kt
import b.B

fun box() = B().test()()

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

class B : A() {
    fun test() = { getO() + getK() }

    private companion object {
        fun getK() = "K"
    }
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
