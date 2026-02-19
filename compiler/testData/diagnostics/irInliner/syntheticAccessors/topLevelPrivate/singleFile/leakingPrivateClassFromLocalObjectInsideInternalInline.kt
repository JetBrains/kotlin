// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-71416
// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

private open class A {
    val ok: String = "OK"
}

private inline fun privateInlineFun() = object : A() {
    fun foo() = super.ok
}.foo()

internal inline fun internalInlineFun() = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING_ERROR!>privateInlineFun()<!>
