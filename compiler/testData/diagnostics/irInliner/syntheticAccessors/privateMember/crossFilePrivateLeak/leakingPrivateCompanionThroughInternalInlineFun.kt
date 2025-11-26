// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-71416
// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE
// DISABLE_IR_VISIBILITY_CHECKS: ANY

// FILE: A.kt
class A {
    private companion object {
        fun foo() = "OK"
    }

    private inline fun privateFun() = foo()
    internal inline fun internalInlineFun() = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING_ERROR!>privateFun()<!>
}

// FILE: main.kt
fun box(): String {
    return A().internalInlineFun()
}
