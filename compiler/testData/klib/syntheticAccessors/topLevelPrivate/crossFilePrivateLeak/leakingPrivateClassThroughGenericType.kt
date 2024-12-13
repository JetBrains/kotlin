// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// ^^^ Muted because a private type is leaked from the declaring file, and the visibility validator detects this.
//     This test should be converted to a test that checks reporting private types exposure. To be done in KT-69681 and KT-71416.

// KT-72862: Undefined symbols
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE

// FILE: a.kt
private class Private

private inline fun <reified T> parameterized(): String {
    if (T::class == Private::class) return "OK"
    return T::class.simpleName ?: "Unknown type"
}

internal inline fun inlineFun() = parameterized<Private>()

// FILE: main.kt
fun box(): String {
    return inlineFun()
}
