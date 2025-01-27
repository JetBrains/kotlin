// ISSUE: KT-71416
// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

// MODULE: lib
// FILE: a.kt
private class Private

private inline fun <reified T> parameterized(): String {
    if (<!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!>T::class<!> == <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!>Private::class<!>) return "OK"
    return <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!>T::class<!>.simpleName ?: "Unknown type"
}

internal inline fun inlineFun() = parameterized<Private>()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return inlineFun()
}
