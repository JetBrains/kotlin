// ISSUE: KT-71416
// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

// MODULE: lib
// FILE: A.kt
private open class A {
    val ok: String = "OK"
}

private inline fun privateInlineFun() = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!>object : A() {
    fun foo() = super.ok
}<!>.foo()

internal inline fun internalInlineFun() = privateInlineFun()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFun()
}
