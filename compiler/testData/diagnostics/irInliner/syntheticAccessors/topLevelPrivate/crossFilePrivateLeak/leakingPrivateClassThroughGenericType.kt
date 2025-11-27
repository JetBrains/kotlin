// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71416
// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE
// DISABLE_IR_VISIBILITY_CHECKS: ANY

// FILE: a.kt
private class Private

private inline fun <reified T> parameterized(): String {
    if (T::class == Private::class) return "OK"
    return T::class.simpleName ?: "Unknown type"
}

internal inline fun inlineFun() = parameterized<<!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>Private<!>>()

// FILE: main.kt
fun box(): String {
    return inlineFun()
}
