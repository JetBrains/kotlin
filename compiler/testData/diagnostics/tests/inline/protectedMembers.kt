// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

// FILE: 1.kt

package test

open class P {
    protected open val FOO = "O"

    protected open fun test() = "K"

    inline fun protectedProp(): String {
        return <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>FOO<!>
    }

    inline fun protectedFun(): String {
        return <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>test<!>()
    }
}

// FILE: 2.kt

import test.*

class A: P() {
    override val FOO: String
        get() = "fail"

    override fun test(): String {
        return "fail"
    }
}

fun box() : String {
    val p = P()
    return p.protectedProp() + p.protectedFun()
}
