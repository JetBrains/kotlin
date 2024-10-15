// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE
// FILE: 1.kt

package test

inline fun runCrossinline(crossinline f: () -> String) = f()

open class Base {
    protected open val FOO = "O"

    protected open fun test() = "K"
}

open class P : Base() {
    inline fun protectedProp(crossinline f: (String) -> String): String =
        runCrossinline { f(<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>FOO<!>) }

    inline fun protectedFun(crossinline f: (String) -> String): String =
        runCrossinline { f(<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>test<!>()) }
}

// FILE: 2.kt

import test.*

fun box() : String {
    val p = P()
    return p.protectedProp { it } + p.protectedFun { it }
}
