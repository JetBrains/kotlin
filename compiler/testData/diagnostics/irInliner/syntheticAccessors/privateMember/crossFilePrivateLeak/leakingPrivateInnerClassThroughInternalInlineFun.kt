// ISSUE: KT-71416
// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

// FILE: A.kt
class A {
    private inner class Inner {
        fun foo() = "OK"
    }

    private inline fun privateFun() = Inner().foo()
    internal inline fun internalInlineFun() = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING_WARNING!>privateFun()<!>
}

// FILE: main.kt
fun box(): String {
    return A().internalInlineFun()
}
