// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS
// IGNORE_INLINER_K2: IR

// FILE: A.kt
private fun privateMethod() = "OK"

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline val publicInlineVal: () -> String
    get() = { privateMethod() }


// FILE: main.kt
fun box(): String {
    return publicInlineVal.invoke()
}
