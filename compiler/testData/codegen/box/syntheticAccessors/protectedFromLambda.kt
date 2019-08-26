// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// FILE: A.kt

package first
import second.C

open class A {
    protected open fun test(): String = "FAIL (A)"
}

fun box() = C().value()

// FILE: B.kt

// See also KT-8344: INVOKESPECIAL instead of INVOKEVIRTUAL in accessor

package second

import first.A

public abstract class B(): A() {
    val value = {
        test()
    }
}

class C: B() {
    override fun test() = "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
