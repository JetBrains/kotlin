// KT-72862: No function found for symbol
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

// MODULE: lib
// FILE: A.kt
private fun privateMethod() = "OK"

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline val publicInlineVal: () -> String
    get() = { privateMethod() }


// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    return publicInlineVal.invoke()
}
