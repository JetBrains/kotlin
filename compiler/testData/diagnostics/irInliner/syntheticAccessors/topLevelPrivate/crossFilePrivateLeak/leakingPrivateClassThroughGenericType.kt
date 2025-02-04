// ISSUE: KT-71416
// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

// FILE: a.kt
private class Private

private inline fun <reified T> parameterized(): String {
    if (T::class == Private::class) return "OK"
    return T::class.simpleName ?: "Unknown type"
}

internal inline fun inlineFun() = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING_WARNING!>parameterized<Private>()<!>

// FILE: main.kt
fun box(): String {
    return inlineFun()
}
