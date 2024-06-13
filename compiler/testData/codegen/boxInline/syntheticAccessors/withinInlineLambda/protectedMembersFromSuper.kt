// LANGUAGE: -ProhibitProtectedCallFromInline
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_MULTI_MODULE_IR_AGAINST_OLD
// FILE: 1.kt

package test

inline fun runCrossinline(crossinline f: () -> String) = f()

open class Base {
    protected open val FOO = "O"

    protected open fun test() = "K"
}

open class P : Base() {
    inline fun protectedProp(crossinline f: (String) -> String): String =
        runCrossinline { f(FOO) }

    inline fun protectedFun(crossinline f: (String) -> String): String =
        runCrossinline { f(test()) }
}

// FILE: 2.kt

import test.*

fun box() : String {
    val p = P()
    return p.protectedProp { it } + p.protectedFun { it }
}
