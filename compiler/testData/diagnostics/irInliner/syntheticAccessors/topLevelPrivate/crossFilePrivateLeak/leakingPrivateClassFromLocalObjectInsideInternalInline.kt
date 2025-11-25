// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-71416
// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE
// DISABLE_IR_VISIBILITY_CHECKS: ANY

// FILE: A.kt
private open class A {
    val ok: String = "OK"
}

private inline fun privateInlineFun() = object : A() {
    fun foo() = super.ok
}.foo()

internal inline fun internalInlineFun() = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING_ERROR!>privateInlineFun()<!>

// FILE: invocation1.kt
fun box1() = internalInlineFun()

// FILE: invocation2.kt
fun box2() = internalInlineFun()
