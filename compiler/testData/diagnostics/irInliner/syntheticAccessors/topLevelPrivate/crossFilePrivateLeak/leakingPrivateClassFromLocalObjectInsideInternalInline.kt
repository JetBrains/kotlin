// ISSUE: KT-71416, KT-74732
// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

// FILE: A.kt
private open class A {
    val ok: String = "OK"
}

// TODO: KT-74732 the diagnostic is reported at the wrong function. Must be reported within `internalInlineFun()` instead
private inline fun privateInlineFun() = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!>object : A() {
    fun foo() = super.ok
}<!>.foo()

internal inline fun internalInlineFun() = privateInlineFun()

// FILE: invocation1.kt
fun box1() = internalInlineFun()

// FILE: invocation2.kt
fun box2() = internalInlineFun()
