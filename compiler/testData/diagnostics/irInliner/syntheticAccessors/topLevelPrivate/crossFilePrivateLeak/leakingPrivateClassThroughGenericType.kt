// ISSUE: KT-71416
// FIR_IDENTICAL

// FILE: a.kt
private class Private

private inline fun <reified T> parameterized(): String {
    if (<!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>T::class<!> == <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>Private::class<!>) return "OK"
    return <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>T::class<!>.simpleName ?: "Unknown type"
}

internal <!NOTHING_TO_INLINE!>inline<!> fun inlineFun() = parameterized<Private>()

// FILE: main.kt
fun box(): String {
    return inlineFun()
}
