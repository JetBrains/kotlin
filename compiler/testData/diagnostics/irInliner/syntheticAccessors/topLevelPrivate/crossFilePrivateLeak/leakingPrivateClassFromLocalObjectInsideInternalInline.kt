// ISSUE: KT-71416
// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

// FILE: A.kt
private open class A {
    val ok: String = "OK"
}

private inline fun privateInlineFun() = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>object : A() {
    fun foo() = super.ok
}<!>.foo()

internal inline fun internalInlineFun() = privateInlineFun()

// FILE: main.kt
fun box(): String {
    return internalInlineFun()
}
